package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        User saved = userService.register(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration successful", null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userService.login(request.getEmail(), request.getPassword());

        if (user.is2faEnabled()) {
            // Generate OTP and email it, don't return token yet
            userService.sendOtp(user);
            return ResponseEntity.accepted().body(new ApiResponse<>(true, "OTP sent to your email",
                    Map.of("requiresOtp", true, "email", user.getEmail())));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", token));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        User user = userService.verifyOtp(email, otp);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", token));
    }

    @PostMapping("/toggle-2fa")
    public ResponseEntity<?> toggle2FA(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean enable = Boolean.parseBoolean(request.get("enable").toString());
        userService.toggle2FA(userId, enable);
        return ResponseEntity.ok(new ApiResponse<>(true, enable ? "2FA enabled" : "2FA disabled", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            userService.resetPassword(request.get("email"));
            return ResponseEntity
                    .ok(new ApiResponse<>(true, "If the email exists, a new password has been sent.", null));
        } catch (Exception e) {
            // Always return success to prevent email enumeration attacks
            return ResponseEntity
                    .ok(new ApiResponse<>(true, "If the email exists, a new password has been sent.", null));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()));
    }
}
