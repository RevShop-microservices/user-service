package com.example.user_service.listener;

import com.example.user_service.dto.NotificationRequest;
import com.example.user_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;

import static org.mockito.Mockito.*;

class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationListener notificationListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleNotificationRequest_NoAttachment() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(1L)
                .userEmail("test@example.com")
                .subject("Hi")
                .message("Hello World")
                .build();

        notificationListener.handleNotificationRequest(request);

        verify(notificationService, times(1)).sendNotification(
                1L,
                "test@example.com",
                "Hi",
                "Hello World"
        );
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void testHandleNotificationRequest_WithAttachment() {
        String base64Content = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        NotificationRequest request = NotificationRequest.builder()
                .userId(2L)
                .userEmail("invoice@example.com")
                .subject("Invoice")
                .message("Attached")
                .attachmentBase64(base64Content)
                .attachmentFilename("invoice.pdf")
                .build();

        notificationListener.handleNotificationRequest(request);

        verify(notificationService, times(1)).sendNotificationWithInvoice(
                2L,
                "invoice@example.com",
                "Invoice",
                "Attached",
                new byte[]{1, 2, 3},
                "invoice.pdf"
        );
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void testHandleNotificationRequest_ExceptionGracefullyHandled() {
        NotificationRequest request = NotificationRequest.builder()
                .userId(1L)
                .userEmail("error@example.com")
                .build();

        doThrow(new RuntimeException("Service failure")).when(notificationService)
                .sendNotification(any(), any(), any(), any());

        // Should not bubble up exception
        notificationListener.handleNotificationRequest(request);

        verify(notificationService, times(1)).sendNotification(any(), any(), any(), any());
    }
}
