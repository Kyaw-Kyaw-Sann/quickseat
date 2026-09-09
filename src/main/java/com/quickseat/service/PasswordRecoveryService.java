package com.quickseat.service;

import com.quickseat.dto.ResetPasswordRequest;
import com.quickseat.dto.VerifyOtpRequest;
import com.quickseat.entity.AuthProvider;
import com.quickseat.entity.PasswordResetOtp;
import com.quickseat.entity.User;
import com.quickseat.exception.BadRequestException;
import com.quickseat.repository.PasswordResetOtpRepository;
import com.quickseat.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private static final int MAX_ATTEMPTS = 5;
    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void requestReset(String email) {
        userRepository.findByEmail(normalize(email)).filter(user -> user.getProvider() == AuthProvider.LOCAL).ifPresent(user -> {
            PasswordResetOtp resetOtp = new PasswordResetOtp();
            resetOtp.setUser(user);
            resetOtp.setOtp(String.format("%06d", secureRandom.nextInt(1_000_000)));
            resetOtp.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            resetOtp.setAttemptCount(0);
            resetOtp.setVerified(false);
            otpRepository.save(resetOtp);
            emailService.send(user.getEmail(), "QuickSeat password reset code",
                    "Your QuickSeat password reset code is " + resetOtp.getOtp() + ". It expires in 10 minutes.");
        });
    }

    public void verifyOtp(VerifyOtpRequest request) {
        PasswordResetOtp resetOtp = currentOtp(request.email());
        validateUsable(resetOtp);
        if (!resetOtp.getOtp().equals(request.otp())) {
            resetOtp.setAttemptCount(resetOtp.getAttemptCount() + 1);
            otpRepository.save(resetOtp);
            if (resetOtp.getAttemptCount() >= MAX_ATTEMPTS) throw new BadRequestException("OTP attempt limit exceeded");
            throw new BadRequestException("Incorrect OTP");
        }
        resetOtp.setVerified(true);
        otpRepository.save(resetOtp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp resetOtp = currentOtp(request.email());
        validateUsable(resetOtp);
        if (!resetOtp.isVerified() || !resetOtp.getOtp().equals(request.otp())) throw new BadRequestException("OTP verification is required");
        User user = resetOtp.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetOtp.setVerified(false);
        resetOtp.setExpiresAt(Instant.now());
    }

    private PasswordResetOtp currentOtp(String email) {
        User user = userRepository.findByEmail(normalize(email)).orElseThrow(() -> new BadRequestException("Invalid password reset request"));
        return otpRepository.findTopByUserOrderByCreatedAtDesc(user).orElseThrow(() -> new BadRequestException("Invalid password reset request"));
    }

    private void validateUsable(PasswordResetOtp resetOtp) {
        if (!resetOtp.getExpiresAt().isAfter(Instant.now())) throw new BadRequestException("OTP has expired");
        if (resetOtp.getAttemptCount() >= MAX_ATTEMPTS) throw new BadRequestException("OTP attempt limit exceeded");
    }

    private String normalize(String email) { return email.trim().toLowerCase(); }
}
