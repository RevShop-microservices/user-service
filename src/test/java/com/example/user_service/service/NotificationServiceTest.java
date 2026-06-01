package com.example.user_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendNotification_UserIdAndEmailPresent() {
        Long userId = 1L;
        String userEmail = "test@example.com";
        String subject = "Test Subject";
        String message = "Test Message";

        notificationService.sendNotification(userId, userEmail, subject, message);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                "1",
                "/queue/notifications",
                message
        );
        verify(emailService, times(1)).sendSimpleMessage(userEmail, subject, message);
    }

    @Test
    void testSendNotification_OnlyUserId() {
        Long userId = 2L;
        String message = "Hello User";

        notificationService.sendNotification(userId, null, null, message);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                "2",
                "/queue/notifications",
                message
        );
        verifyNoInteractions(emailService);
    }

    @Test
    void testSendNotification_OnlyEmail() {
        String email = "only@example.com";
        String subject = "Hello";
        String message = "Only Email";

        notificationService.sendNotification(null, email, subject, message);

        verifyNoInteractions(messagingTemplate);
        verify(emailService, times(1)).sendSimpleMessage(email, subject, message);
    }

    @Test
    void testSendNotification_WebSocketThrowsException() {
        Long userId = 3L;
        String email = "fail@example.com";
        String message = "Failure test";

        doThrow(new RuntimeException("WebSocket Error")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        // Should not throw exception to caller
        notificationService.sendNotification(userId, email, "Subj", message);

        verify(emailService, times(1)).sendSimpleMessage(email, "Subj", message);
    }

    @Test
    void testSendNotificationWithInvoice_Success() {
        Long userId = 4L;
        String email = "invoice@example.com";
        String subject = "Invoice";
        String message = "Invoice attached";
        byte[] attachment = {4, 5, 6};
        String filename = "invoice.pdf";

        notificationService.sendNotificationWithInvoice(userId, email, subject, message, attachment, filename);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                "4",
                "/queue/notifications",
                message
        );
        verify(emailService, times(1)).sendEmailWithAttachment(email, subject, message, attachment, filename);
    }
}
