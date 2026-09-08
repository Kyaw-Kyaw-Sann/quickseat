package com.quickseat.security;

import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service public class RevokedTokenService {
    private final ConcurrentHashMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    public void revoke(Claims claims) { revokedTokens.put(claims.getId(), claims.getExpiration().toInstant()); }
    public boolean isRevoked(Claims claims) { revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(Instant.now())); return revokedTokens.containsKey(claims.getId()); }
}
