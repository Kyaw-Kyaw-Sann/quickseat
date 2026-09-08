package com.quickseat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key; private final long accessMinutes; private final long refreshDays;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-token-minutes}") long accessMinutes,
                      @Value("${app.jwt.refresh-token-days}") long refreshDays) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.accessMinutes = accessMinutes; this.refreshDays = refreshDays;
    }
    public String createAccessToken(AppUserDetails user) { return create(user, "access", Instant.now().plus(accessMinutes, ChronoUnit.MINUTES)); }
    public String createRefreshToken(AppUserDetails user) { return create(user, "refresh", Instant.now().plus(refreshDays, ChronoUnit.DAYS)); }
    private String create(AppUserDetails user, String type, Instant expiry) { return Jwts.builder().subject(user.username()).claim("type", type).id(UUID.randomUUID().toString()).issuedAt(new Date()).expiration(Date.from(expiry)).signWith(key).compact(); }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public boolean isRefreshToken(Claims claims) { return "refresh".equals(claims.get("type", String.class)); }
}
