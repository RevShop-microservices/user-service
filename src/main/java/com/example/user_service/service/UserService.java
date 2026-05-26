package com.example.user_service.service;

import com.example.user_service.client.SellerClient;
import com.example.user_service.dto.SellerAnalyticsDTO;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private SellerClient sellerClient;

    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists");
        }
        // Backend password validation (defence even if frontend is bypassed)
        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Normalize role
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("CUSTOMER");
        } else {
            String role = user.getRole().toUpperCase();
            if (role.startsWith("ROLE_")) role = role.substring(5);
            user.setRole(role);
        }

        User savedUser = userRepository.save(user);

        // Send registration notification
        notificationService.sendNotification(
                savedUser.getId(),
                savedUser.getEmail(),
                "Welcome to NexShop!",
                "Hello " + savedUser.getName()
                        + ",\n\nThank you for registering at NexShop. We're excited to have you!");

        return savedUser;
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        if ("SELLER".equalsIgnoreCase(user.getRole())) {
            try {
                if (sellerClient != null) {
                    SellerAnalyticsDTO analytics = sellerClient.getSellerAnalytics(user.getId());
                    String bestProduct = "N/A";
                    if (analytics.getTopSellingProducts() != null && !analytics.getTopSellingProducts().isEmpty()) {
                        bestProduct = analytics.getTopSellingProducts().get(0).getProductName();
                    }
                    notificationService.sendNotification(
                            user.getId(),
                            user.getEmail(),
                            "New Login Alert & Analytics",
                            "Hello " + user.getName()
                                    + ",\n\nYou have successfully logged into your NexShop Seller Dashboard.\n" +
                                    "Your current best-selling product is: " + bestProduct + "\n\nKeep up the great work!");
                }
            } catch (Exception e) {
                // Ignore failure so login isn't blocked
            }
        }
        return user;
    }

    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        // Generate random 10-char password satisfying constraints
        String tempPassword = "Nex" + (int) (Math.random() * 10000) + "@Pwd!";

        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        notificationService.sendNotification(
                user.getId(),
                user.getEmail(),
                "Your NexShop Password Has Been Reset",
                "Hello " + user.getName() + ",\n\nYour password has been reset.\nYour new temporary password is: "
                        + tempPassword + "\n\nPlease login and change it immediately.");
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new RuntimeException("Password must contain at least one special character");
        }
    }

    // ── 2FA Methods ──

    public void sendOtp(User user) {
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setCurrentOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        notificationService.sendNotification(
                user.getId(),
                user.getEmail(),
                "Your NexShop OTP Code",
                "Hello " + user.getName() + ",\n\nYour one-time password is: " + otp
                        + "\n\nThis code expires in 5 minutes.");
    }

    public User verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        if (user.getCurrentOtp() == null || !user.getCurrentOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please login again.");
        }

        // Clear OTP after successful verification
        user.setCurrentOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        return user;
    }

    public void toggle2FA(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.set2faEnabled(enable);
        userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}