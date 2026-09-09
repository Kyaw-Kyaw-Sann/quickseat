package com.quickseat.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.quickseat.entity.EmailVerificationToken;
import com.quickseat.entity.User;
import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.EmailVerificationTokenRepository;
import com.quickseat.repository.UserRepository;
import com.quickseat.service.shared.EmailService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    @Mock EmailVerificationTokenRepository tokenRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    EmailVerificationService service;

    @BeforeEach void setUp() {
        service = new EmailVerificationService(tokenRepository, userRepository, emailService);
        ReflectionTestUtils.setField(service, "verificationBaseUrl", "http://localhost:8080/api/v1/auth/verify-email");
    }

    @Test void createsAndResendsVerificationToken() {
        User user = user(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of());
        service.resend(user.getEmail());
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
        verify(emailService).send(eq(user.getEmail()), anyString(),
                contains("http://localhost:8080/api/v1/auth/verify-email?token=" + captor.getValue().getToken()));
    }

    @Test void verifiesValidTokenAndRejectsUsedOrExpiredTokens() {
        User user = user(false);
        EmailVerificationToken valid = token(user, Instant.now().plusSeconds(60), false);
        when(tokenRepository.findByToken("valid")).thenReturn(Optional.of(valid));
        service.verify("valid");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(valid.isUsed()).isTrue();

        when(tokenRepository.findByToken("used")).thenReturn(Optional.of(token(user, Instant.now().plusSeconds(60), true)));
        when(tokenRepository.findByToken("expired")).thenReturn(Optional.of(token(user, Instant.now().minusSeconds(1), false)));
        assertThatThrownBy(() -> service.verify("used")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.verify("expired")).isInstanceOf(BadRequestException.class);
    }

    private User user(boolean verified) { User user = new User(); user.setId(1L); user.setEmail("user@example.com"); user.setEmailVerified(verified); return user; }
    private EmailVerificationToken token(User user, Instant expiry, boolean used) { EmailVerificationToken token = new EmailVerificationToken(); token.setUser(user); token.setExpiresAt(expiry); token.setUsed(used); return token; }
}
