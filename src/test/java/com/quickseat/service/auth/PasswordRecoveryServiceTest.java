package com.quickseat.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quickseat.dto.request.auth.ResetPasswordRequest;
import com.quickseat.dto.request.auth.VerifyOtpRequest;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.entity.PasswordResetOtp;
import com.quickseat.entity.User;
import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.PasswordResetOtpRepository;
import com.quickseat.repository.UserRepository;
import com.quickseat.service.shared.EmailService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordResetOtpRepository otpRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    PasswordRecoveryService service;
    User user;

    @BeforeEach void setUp() {
        service = new PasswordRecoveryService(userRepository, otpRepository, passwordEncoder, emailService);
        user = new User(); user.setId(1L); user.setEmail("user@example.com"); user.setProvider(AuthProvider.LOCAL);
    }

    @Test void requestCreatesSixDigitOtpAndSendsEmail() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        service.requestReset(user.getEmail());
        ArgumentCaptor<PasswordResetOtp> captor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getOtp()).matches("\\d{6}");
        verify(emailService).send(eq(user.getEmail()), anyString(), contains(captor.getValue().getOtp()));
    }

    @Test void expiryAndAttemptLimitAreEnforced() {
        PasswordResetOtp otp = otp("123456", Instant.now().minusSeconds(1));
        mockCurrent(otp);
        assertThatThrownBy(() -> service.verifyOtp(new VerifyOtpRequest(user.getEmail(), "123456"))).isInstanceOf(BadRequestException.class);

        otp.setExpiresAt(Instant.now().plusSeconds(60)); otp.setAttemptCount(4);
        assertThatThrownBy(() -> service.verifyOtp(new VerifyOtpRequest(user.getEmail(), "000000"))).isInstanceOf(BadRequestException.class);
        assertThat(otp.getAttemptCount()).isEqualTo(5);
    }

    @Test void verifiedOtpResetsPasswordAndCannotBeReused() {
        PasswordResetOtp otp = otp("123456", Instant.now().plusSeconds(60));
        mockCurrent(otp);
        service.verifyOtp(new VerifyOtpRequest(user.getEmail(), "123456"));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-password");
        service.resetPassword(new ResetPasswordRequest(user.getEmail(), "123456", "newPassword123"));
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(otp.isVerified()).isFalse();
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(user.getEmail(), "123456", "anotherPassword123"))).isInstanceOf(BadRequestException.class);
    }

    private void mockCurrent(PasswordResetOtp otp) { when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user)); when(otpRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp)); }
    private PasswordResetOtp otp(String value, Instant expiry) { PasswordResetOtp otp = new PasswordResetOtp(); otp.setUser(user); otp.setOtp(value); otp.setExpiresAt(expiry); return otp; }
}
