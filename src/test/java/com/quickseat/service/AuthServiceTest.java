package com.quickseat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.quickseat.dto.LoginRequest;
import com.quickseat.dto.RefreshTokenRequest;
import com.quickseat.dto.RegisterRequest;
import com.quickseat.entity.AuthProvider;
import com.quickseat.entity.Role;
import com.quickseat.entity.User;
import com.quickseat.exception.UnauthorizedException;
import com.quickseat.repository.UserRepository;
import com.quickseat.security.JwtService;
import com.quickseat.security.RevokedTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationService emailVerificationService;
    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;
    @BeforeEach void setUp() { passwordEncoder = new BCryptPasswordEncoder(); authService = new AuthService(userRepository, passwordEncoder, new JwtService("test-secret-that-is-longer-than-thirty-two-characters", 15, 7), new RevokedTokenService(), emailVerificationService); }
    @Test void registerHashesPasswordAndReturnsTokens() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> { User user = invocation.getArgument(0); user.setId(1L); return user; });
        var response = authService.register(new RegisterRequest("Customer", "customer@example.com", "password123", null));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER); assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(passwordEncoder.matches("password123", captor.getValue().getPassword())).isTrue(); assertThat(response.accessToken()).isNotBlank();
    }
    @Test void loginRefreshAndLogoutInvalidateRefreshToken() {
        User user = user("password123", true); when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        var login = authService.login(new LoginRequest("customer@example.com", "password123"));
        var refreshed = authService.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertThat(refreshed.accessToken()).isNotEqualTo(login.accessToken());
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(login.refreshToken()))).isInstanceOf(UnauthorizedException.class);
    }
    @Test void disabledUserCannotLogin() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user("password123", false)));
        assertThatThrownBy(() -> authService.login(new LoginRequest("customer@example.com", "password123"))).isInstanceOf(UnauthorizedException.class);
    }
    private User user(String rawPassword, boolean active) { User user = new User(); user.setId(1L); user.setName("Customer"); user.setEmail("customer@example.com"); user.setPassword(passwordEncoder.encode(rawPassword)); user.setRole(Role.CUSTOMER); user.setProvider(AuthProvider.LOCAL); user.setActive(active); return user; }
}
