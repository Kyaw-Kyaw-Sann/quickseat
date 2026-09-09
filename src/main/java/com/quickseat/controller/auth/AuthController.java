package com.quickseat.controller.auth;

import com.quickseat.dto.common.ApiResponse;
import com.quickseat.dto.request.auth.EmailRequest;
import com.quickseat.dto.request.auth.LoginRequest;
import com.quickseat.dto.request.auth.LogoutRequest;
import com.quickseat.dto.request.auth.RefreshTokenRequest;
import com.quickseat.dto.request.auth.RegisterRequest;
import com.quickseat.dto.request.auth.ResetPasswordRequest;
import com.quickseat.dto.request.auth.VerifyOtpRequest;
import com.quickseat.dto.response.auth.AuthResponse;
import com.quickseat.service.auth.AuthService;
import com.quickseat.service.auth.EmailVerificationService;
import com.quickseat.service.auth.PasswordRecoveryService;
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

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ApiResponse.success("Email verified successfully. You can now continue using QuickSeat.", null);
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
