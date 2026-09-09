package com.quickseat.controller;

import com.quickseat.dto.*;
import com.quickseat.service.AuthService;
import com.quickseat.service.EmailVerificationService;
import com.quickseat.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("Logout successful", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return ApiResponse.success("Email verified successfully", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.resend(request.email());
        return ApiResponse.success("If the account is eligible, a verification email has been sent", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordRecoveryService.requestReset(request.email());
        return ApiResponse.success("If the account exists, a reset code has been sent", null);
    }

    @PostMapping("/verify-reset-otp")
    public ApiResponse<Void> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        passwordRecoveryService.verifyOtp(request);
        return ApiResponse.success("OTP verified successfully", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ApiResponse.success("Password reset successfully", null);
    }
}
