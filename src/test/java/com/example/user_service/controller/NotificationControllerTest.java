package com.example.user_service.controller;

import com.example.user_service.dto.NotificationRequest;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        }
)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtUtil jwtUtil; // Required by JwtFilter / SecurityConfig wiring

    @Test
    void testSendNotification_Success() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .userId(1L)
                .userEmail("user@example.com")
                .subject("Subject")
                .message("Message content")
                .build();

        mockMvc.perform(post("/api/auth/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).sendNotification(
                eq(1L),
                eq("user@example.com"),
                eq("Subject"),
                eq("Message content")
        );
    }

    @Test
    void testSendNotificationWithAttachment_Success() throws Exception {
        String base64Content = Base64.getEncoder().encodeToString(new byte[]{7, 8, 9});
        NotificationRequest request = NotificationRequest.builder()
                .userId(2L)
                .userEmail("invoice@example.com")
                .subject("Invoice")
                .message("Here is your invoice")
                .attachmentBase64(base64Content)
                .attachmentFilename("invoice.pdf")
                .build();

        mockMvc.perform(post("/api/auth/notifications/send-with-attachment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).sendNotificationWithInvoice(
                eq(2L),
                eq("invoice@example.com"),
                eq("Invoice"),
                eq("Here is your invoice"),
                eq(new byte[]{7, 8, 9}),
                eq("invoice.pdf")
        );
    }
}
