package com.quickseat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickseat.dto.response.auth.AuthResponse;
import com.quickseat.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");
        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            response.sendRedirect(loginErrorUrl());
            return;
        }
        AuthResponse authResponse = authService.loginWithGoogle(email, oauthUser.getAttribute("name"));
        String sessionPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                objectMapper.writeValueAsString(authResponse).getBytes(StandardCharsets.UTF_8));
        response.sendRedirect(callbackUrl() + "#session=" + sessionPayload);
    }

    private String callbackUrl() {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/auth/oauth/callback")
                .build()
                .toUriString();
    }

    private String loginErrorUrl() {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/login")
                .queryParam("oauthError", "google_email_not_verified")
                .build()
                .toUriString();
    }
}
