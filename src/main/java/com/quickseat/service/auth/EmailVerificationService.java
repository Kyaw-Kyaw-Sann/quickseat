package com.quickseat.service.auth;

import com.quickseat.entity.EmailVerificationToken;
import com.quickseat.entity.User;
import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.EmailVerificationTokenRepository;
import com.quickseat.repository.UserRepository;
import com.quickseat.service.shared.EmailService;
import java.security.SecureRandom;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.verification.base-url}") private String verificationBaseUrl;

    @Transactional
    public void createAndSend(User user) {
        if (user.isEmailVerified()) return;
        tokenRepository.findAllByUserAndUsedFalse(user).forEach(token -> token.setUsed(true));
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(generateToken());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(token);
        String separator = verificationBaseUrl.contains("?") ? "&" : "?";
        String link = verificationBaseUrl + separator + "token="
                + URLEncoder.encode(token.getToken(), StandardCharsets.UTF_8);
        emailService.send(user.getEmail(), "Verify your QuickSeat email",
                "Welcome to QuickSeat. Verify your email using this link:\n" + link + "\nThis link expires in 24 hours.");
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));
        if (token.isUsed()) throw new BadRequestException("Verification token has already been used");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new BadRequestException("Verification token has expired");
        token.setUsed(true);
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void resend(String email) {
        userRepository.findByEmail(normalize(email)).ifPresent(this::createAndSend);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalize(String email) { return email.trim().toLowerCase(); }
}
