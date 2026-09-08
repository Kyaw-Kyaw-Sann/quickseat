package com.quickseat.service;

import com.quickseat.dto.*;
import com.quickseat.entity.AuthProvider;
import com.quickseat.entity.Role;
import com.quickseat.entity.User;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.UnauthorizedException;
import com.quickseat.repository.UserRepository;
import com.quickseat.security.AppUserDetails;
import com.quickseat.security.JwtService;
import com.quickseat.security.RevokedTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository; private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; private final RevokedTokenService revokedTokenService;
    @Transactional public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) throw new ConflictException("Email is already registered");
        User user = new User(); user.setName(request.name().trim()); user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password())); user.setPhone(request.phone()); user.setRole(Role.CUSTOMER);
        user.setProvider(AuthProvider.LOCAL); user.setActive(true); user.setEmailVerified(false);
        return tokens(userRepository.save(user));
    }
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase()).orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!user.isActive()) throw new UnauthorizedException("This account is disabled");
        if (!passwordEncoder.matches(request.password(), user.getPassword())) throw new UnauthorizedException("Invalid email or password");
        return tokens(user);
    }
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = refreshClaims(request.refreshToken()); revokedTokenService.revoke(claims);
        User user = userRepository.findByEmail(claims.getSubject()).orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (!user.isActive()) throw new UnauthorizedException("This account is disabled"); return tokens(user);
    }
    public void logout(LogoutRequest request) { revokedTokenService.revoke(refreshClaims(request.refreshToken())); }
    private Claims refreshClaims(String token) {
        try { Claims claims = jwtService.parse(token); if (!jwtService.isRefreshToken(claims) || revokedTokenService.isRevoked(claims)) throw new UnauthorizedException("Invalid refresh token"); return claims; }
        catch (JwtException | IllegalArgumentException exception) { throw new UnauthorizedException("Invalid refresh token"); }
    }
    private AuthResponse tokens(User user) { AppUserDetails principal = AppUserDetails.from(user); return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), jwtService.createAccessToken(principal), jwtService.createRefreshToken(principal), "Bearer"); }
}
