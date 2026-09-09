package com.quickseat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickseat.dto.ApiResponse;
import com.quickseat.dto.AuthResponse;
import com.quickseat.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");
        if (email == null || !Boolean.TRUE.equals(emailVerified)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google email is not verified");
            return;
        }
        AuthResponse authResponse = authService.loginWithGoogle(email, oauthUser.getAttribute("name"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.success("Google login successful", authResponse));
    }
}
