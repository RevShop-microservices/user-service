package com.example.user_service.listener;

import com.example.user_service.dto.NotificationRequest;
import com.example.user_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = "notification-queue")
    public void handleNotificationRequest(NotificationRequest request) {
        log.info("Received notification request via RabbitMQ for user email: {}", request.getUserEmail());
        try {
            if (request.getAttachmentBase64() != null && !request.getAttachmentBase64().isEmpty()) {
                byte[] attachmentBytes = Base64.getDecoder().decode(request.getAttachmentBase64());
                notificationService.sendNotificationWithInvoice(
                        request.getUserId(),
                        request.getUserEmail(),
                        request.getSubject(),
                        request.getMessage(),
                        attachmentBytes,
                        request.getAttachmentFilename());
            } else {
                notificationService.sendNotification(
                        request.getUserId(),
                        request.getUserEmail(),
                        request.getSubject(),
                        request.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to process notification request via RabbitMQ", e);
        }
    }
}
