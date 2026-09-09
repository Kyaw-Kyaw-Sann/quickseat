package com.quickseat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
}
