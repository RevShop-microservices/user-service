package com.example.user_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Value("${spring.mail.from:noreply@nexshop.com}")
    private String fromAddress;

    public void sendSimpleMessage(String to, String subject, String text) {
        if (emailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping email to {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String text, byte[] attachmentBytes, String attachmentName) {
        if (emailSender == null) {
            log.warn("JavaMailSender is not configured. Skipping email with attachment to {}", to);
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage message = emailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            if (attachmentBytes != null) {
                org.springframework.core.io.ByteArrayResource byteArrayResource =
                        new org.springframework.core.io.ByteArrayResource(attachmentBytes);
                helper.addAttachment(attachmentName, byteArrayResource);
            }

            emailSender.send(message);
            log.info("Email with attachment sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}", to, e);
        }
    }
}
