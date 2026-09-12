package com.quickseat.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${app.mail.enabled:false}") private boolean mailEnabled;
    @Value("${spring.mail.username:}") private String fromAddress;

    public void send(String recipient, String subject, String body) {
        if (!mailEnabled) {
            log.info("Email delivery skipped because MAIL_ENABLED is false");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email notification sent successfully");
    }

    public void sendWithAttachment(String recipient, String subject, String body,
                                   String fileName, byte[] content, String contentType) {
        if (!mailEnabled) {
            log.info("Email delivery skipped because MAIL_ENABLED is false");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(fileName, new ByteArrayResource(content), contentType);
            mailSender.send(message);
            log.info("Email notification with attachment sent successfully");
        } catch (MessagingException exception) {
            throw new MailSendException("Could not prepare email attachment", exception);
        }
    }
}
