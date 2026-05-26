package com.example.user_service.controller;

import com.example.user_service.dto.NotificationRequest;
import com.example.user_service.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/auth/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
        notificationService.sendNotification(
                request.getUserId(),
                request.getUserEmail(),
                request.getSubject(),
                request.getMessage());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-with-attachment")
    public ResponseEntity<?> sendNotificationWithAttachment(@RequestBody NotificationRequest request) {
        byte[] attachmentBytes = null;
        if (request.getAttachmentBase64() != null && !request.getAttachmentBase64().isEmpty()) {
            attachmentBytes = Base64.getDecoder().decode(request.getAttachmentBase64());
        }
        notificationService.sendNotificationWithInvoice(
                request.getUserId(),
                request.getUserEmail(),
                request.getSubject(),
                request.getMessage(),
                attachmentBytes,
                request.getAttachmentFilename());
        return ResponseEntity.ok().build();
    }
}
