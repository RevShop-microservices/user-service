package com.example.user_service.service;

import com.example.user_service.client.SellerClient;
import com.example.user_service.dto.SellerAnalyticsDTO;
import com.example.user_service.dto.TopProductDTO;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SellerClient sellerClient;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ── REGISTER TESTS ──

    @Test
    void testRegister_Success_DefaultRole() {
        User inputUser = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("Password123!") // Valid password
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("encodedPassword")
                .role("CUSTOMER")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(inputUser.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(inputUser);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("CUSTOMER", result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
        verify(notificationService, times(1)).sendNotification(
                eq(1L),
                eq("test@example.com"),
                eq("Welcome to NexShop!"),
                anyString()
        );
    }

    @Test
    void testRegister_Success_StripRolePrefix() {
        User inputUser = User.builder()
                .email("seller@example.com")
                .name("Seller User")
                .password("Password123!")
                .role("ROLE_SELLER")
                .build();

        User savedUser = User.builder()
                .id(2L)
                .email("seller@example.com")
                .name("Seller User")
                .password("encodedPassword")
                .role("SELLER")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(inputUser.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(inputUser);

        assertNotNull(result);
        assertEquals("SELLER", result.getRole());
    }

    @Test
    void testRegister_Fail_EmailAlreadyExists() {
        User inputUser = User.builder()
                .email("existing@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertEquals("An account with this email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_Fail_PasswordTooShort() {
        User inputUser = User.builder()
                .email("short@example.com")
                .password("P1!")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertTrue(exception.getMessage().contains("at least 8 characters"));
    }

    @Test
    void testRegister_Fail_PasswordNoUppercase() {
        User inputUser = User.builder()
                .email("test@example.com")
                .password("password123!")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertTrue(exception.getMessage().contains("at least one uppercase letter"));
    }

    @Test
    void testRegister_Fail_PasswordNoLowercase() {
        User inputUser = User.builder()
                .email("test@example.com")
                .password("PASSWORD123!")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertTrue(exception.getMessage().contains("at least one lowercase letter"));
    }

    @Test
    void testRegister_Fail_PasswordNoDigit() {
        User inputUser = User.builder()
                .email("test@example.com")
                .password("Password@@@")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertTrue(exception.getMessage().contains("at least one number"));
    }

    @Test
    void testRegister_Fail_PasswordNoSpecialChar() {
        User inputUser = User.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.register(inputUser));
        assertTrue(exception.getMessage().contains("at least one special character"));
    }

    // ── LOGIN TESTS ──

    @Test
    void testLogin_Success_Customer() {
        String email = "customer@example.com";
        String password = "Password123!";

        User mockUser = User.builder()
                .id(1L)
                .email(email)
                .password("encodedPassword")
                .role("CUSTOMER")
                .name("Cust")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);

        User result = userService.login(email, password);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verifyNoInteractions(sellerClient);
    }

    @Test
    void testLogin_Success_Seller_WithAnalytics() {
        String email = "seller@example.com";
        String password = "Password123!";

        User mockUser = User.builder()
                .id(2L)
                .email(email)
                .password("encodedPassword")
                .role("SELLER")
                .name("Sell")
                .build();

        TopProductDTO topProduct = new TopProductDTO("prod-1", "Premium Laptop", 100);
        SellerAnalyticsDTO analytics = SellerAnalyticsDTO.builder()
                .sellerId(2L)
                .topSellingProducts(Collections.singletonList(topProduct))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
        when(sellerClient.getSellerAnalytics(2L)).thenReturn(analytics);

        User result = userService.login(email, password);

        assertNotNull(result);
        verify(sellerClient, times(1)).getSellerAnalytics(2L);
        verify(notificationService, times(1)).sendNotification(
                eq(2L),
                eq(email),
                eq("New Login Alert & Analytics"),
                contains("Premium Laptop")
        );
    }

    @Test
    void testLogin_Success_Seller_AnalyticsThrowsException() {
        String email = "seller@example.com";
        String password = "Password123!";

        User mockUser = User.builder()
                .id(2L)
                .email(email)
                .password("encodedPassword")
                .role("SELLER")
                .name("Sell")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
        when(sellerClient.getSellerAnalytics(2L)).thenThrow(new RuntimeException("Seller Service Offline"));

        // Login should NOT fail even if analytics retrieval fails
        User result = userService.login(email, password);

        assertNotNull(result);
        verify(sellerClient, times(1)).getSellerAnalytics(2L);
        verify(notificationService, never()).sendNotification(any(), any(), eq("New Login Alert & Analytics"), any());
    }

    @Test
    void testLogin_Fail_UserNotFound() {
        when(userRepository.findByEmail("non@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.login("non@example.com", "any"));
    }

    @Test
    void testLogin_Fail_IncorrectPassword() {
        String email = "user@example.com";
        User mockUser = User.builder()
                .email(email)
                .password("encoded")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.login(email, "wrong"));
    }

    // ── PASSWORD RESET TESTS ──

    @Test
    void testResetPassword_Success() {
        String email = "reset@example.com";
        User mockUser = User.builder()
                .id(10L)
                .email(email)
                .name("Reset User")
                .password("oldEncoded")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

        userService.resetPassword(email);

        verify(userRepository, times(1)).save(mockUser);
        verify(notificationService, times(1)).sendNotification(
                eq(10L),
                eq(email),
                eq("Your NexShop Password Has Been Reset"),
                contains("Your new temporary password is")
        );
        assertEquals("newEncodedPassword", mockUser.getPassword());
    }

    @Test
    void testResetPassword_UserNotFound() {
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.resetPassword("none@example.com"));
    }

    // ── OTP & 2FA TESTS ──

    @Test
    void testSendOtp() {
        User user = User.builder()
                .id(5L)
                .email("otp@example.com")
                .name("Otp User")
                .build();

        userService.sendOtp(user);

        assertNotNull(user.getCurrentOtp());
        assertEquals(6, user.getCurrentOtp().length());
        assertNotNull(user.getOtpExpiry());
        verify(userRepository, times(1)).save(user);
        verify(notificationService, times(1)).sendNotification(
                eq(5L),
                eq("otp@example.com"),
                eq("Your NexShop OTP Code"),
                contains(user.getCurrentOtp())
        );
    }

    @Test
    void testVerifyOtp_Success() {
        String email = "verify@example.com";
        User user = User.builder()
                .id(6L)
                .email(email)
                .currentOtp("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(2))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User verifiedUser = userService.verifyOtp(email, "123456");

        assertNotNull(verifiedUser);
        assertNull(verifiedUser.getCurrentOtp());
        assertNull(verifiedUser.getOtpExpiry());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        String email = "verify@example.com";
        User user = User.builder()
                .email(email)
                .currentOtp("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(2))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> userService.verifyOtp(email, "999999"));
    }

    @Test
    void testVerifyOtp_ExpiredOtp() {
        String email = "verify@example.com";
        User user = User.builder()
                .email(email)
                .currentOtp("123456")
                .otpExpiry(LocalDateTime.now().minusMinutes(1)) // Expired 1 min ago
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> userService.verifyOtp(email, "123456"));
    }

    @Test
    void testToggle2FA() {
        User user = User.builder()
                .id(1L)
                .is2faEnabled(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.toggle2FA(1L, true);

        assertTrue(user.is2faEnabled());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testGetUserById_Success() {
        User user = User.builder().id(12L).build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(12L);
        assertEquals(12L, result.getId());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
    }
}
