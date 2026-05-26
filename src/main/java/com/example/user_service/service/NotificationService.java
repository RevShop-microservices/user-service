package com.example.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    public void sendNotification(Long userId, String userEmail, String subject, String message) {
        // Send via WebSocket to specific user queue
        if (userId != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/notifications",
                        message);
            } catch (Exception e) {
                log.error("WebSocket notification failed for user {}", userId, e);
            }
        }

        // Send via Email (non-blocking)
        if (userEmail != null && !userEmail.isEmpty()) {
            sendEmailAsync(userEmail, subject, message);
        }
    }

    public void sendNotificationWithInvoice(Long userId, String userEmail, String subject, String message, byte[] invoiceBytes, String filename) {
        // Send via WebSocket to specific user queue
        if (userId != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/notifications",
                        message);
            } catch (Exception e) {
                log.error("WebSocket notification failed for user {}", userId, e);
            }
        }

        // Send via Email (non-blocking) with attachment
        if (userEmail != null && !userEmail.isEmpty()) {
            sendEmailWithAttachmentAsync(userEmail, subject, message, invoiceBytes, filename);
        }
    }

    @Async
    public void sendEmailAsync(String to, String subject, String message) {
        try {
            emailService.sendSimpleMessage(to, subject, message);
        } catch (Exception e) {
            log.error("Email notification failed for {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendEmailWithAttachmentAsync(String to, String subject, String message, byte[] attachment, String filename) {
        try {
            emailService.sendEmailWithAttachment(to, subject, message, attachment, filename);
        } catch (Exception e) {
            log.error("Email notification with attachment failed for {}: {}", to, e.getMessage());
        }
    }
}
