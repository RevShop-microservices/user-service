package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testRegister_Success() throws Exception {
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("Password123!")
                .build();

        when(userService.register(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"));

        verify(userService, times(1)).register(any(User.class));
    }

    @Test
    void testRegister_ValidationError() throws Exception {
        // Name is blank, email invalid, password invalid (validation rules from User model)
        User invalidUser = User.builder()
                .name("")
                .email("invalid-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void testLogin_Success_No2fa() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("john@example.com");
        req.setPassword("Password123!");
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role("CUSTOMER")
                .is2faEnabled(false)
                .build();

        when(userService.login(req.getEmail(), req.getPassword())).thenReturn(user);
        when(jwtUtil.generateToken("john@example.com", "CUSTOMER", 1L)).thenReturn("mockToken");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data").value("mockToken"));
    }

    @Test
    void testLogin_Success_With2fa() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("john@example.com");
        req.setPassword("Password123!");
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role("CUSTOMER")
                .is2faEnabled(true)
                .build();

        when(userService.login(req.getEmail(), req.getPassword())).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent to your email"))
                .andExpect(jsonPath("$.data.requiresOtp").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));

        verify(userService, times(1)).sendOtp(user);
    }

    @Test
    void testVerifyOtp_Success() throws Exception {
        Map<String, String> request = Map.of("email", "john@example.com", "otp", "123456");
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role("CUSTOMER")
                .build();

        when(userService.verifyOtp("john@example.com", "123456")).thenReturn(user);
        when(jwtUtil.generateToken("john@example.com", "CUSTOMER", 1L)).thenReturn("jwtToken");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("jwtToken"));
    }

    @Test
    void testToggle2FA_Success() throws Exception {
        Map<String, Object> request = Map.of("userId", 1L, "enable", true);

        mockMvc.perform(post("/api/auth/toggle-2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("2FA enabled"));

        verify(userService, times(1)).toggle2FA(1L, true);
    }

    @Test
    void testForgotPassword_Success_Always() throws Exception {
        Map<String, String> request = Map.of("email", "notfound@example.com");

        doThrow(new RuntimeException("User not found")).when(userService).resetPassword("notfound@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        User user = User.builder()
                .id(99L)
                .name("Alex")
                .email("alex@example.com")
                .role("ADMIN")
                .build();

        when(userService.getUserById(99L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
