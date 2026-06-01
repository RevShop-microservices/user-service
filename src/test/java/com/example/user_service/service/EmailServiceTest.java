package com.example.user_service.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @InjectMocks
    private EmailService emailService;

    private final String fromAddress = "noreply@nexshop.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(emailService, "fromAddress", fromAddress);
    }

    @Test
    void testSendSimpleMessage_SenderNull() {
        // Set emailSender to null explicitly
        ReflectionTestUtils.setField(emailService, "emailSender", null);

        // Should exit gracefully without throwing exception
        emailService.sendSimpleMessage("test@example.com", "Subject", "Body");

        // Verify that no interactions happened on the mocked sender
        // (since reflection setting was used, we check if mock doesn't get called if it was mock,
        // but here it is null, so it shouldn't raise NullPointerException)
    }

    @Test
    void testSendSimpleMessage_Success() {
        String to = "user@example.com";
        String subject = "Test Subject";
        String text = "Test Body";

        emailService.sendSimpleMessage(to, subject, text);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals(fromAddress, capturedMessage.getFrom());
        assertEquals(to, capturedMessage.getTo()[0]);
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(text, capturedMessage.getText());
    }

    @Test
    void testSendEmailWithAttachment_SenderNull() {
        ReflectionTestUtils.setField(emailService, "emailSender", null);

        emailService.sendEmailWithAttachment("test@example.com", "Subject", "Body", new byte[]{1, 2, 3}, "file.txt");
    }

    @Test
    void testSendEmailWithAttachment_Success() throws Exception {
        String to = "user@example.com";
        String subject = "Invoice Subject";
        String text = "Please find attached";
        byte[] attachment = {1, 2, 3};
        String filename = "invoice.pdf";

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(emailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        emailService.sendEmailWithAttachment(to, subject, text, attachment, filename);

        verify(emailSender, times(1)).createMimeMessage();
        verify(emailSender, times(1)).send(mockMimeMessage);
    }
}
