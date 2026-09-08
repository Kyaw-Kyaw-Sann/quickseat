package com.quickseat.security;

import com.quickseat.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final UserRepository userRepository;
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try {
            Claims claims = jwtService.parse(header.substring(7));
            if ("access".equals(claims.get("type", String.class))) userRepository.findByEmail(claims.getSubject()).filter(User -> User.isActive()).ifPresent(user -> {
                AppUserDetails principal = AppUserDetails.from(user);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
            });
        } catch (JwtException | IllegalArgumentException ignored) { SecurityContextHolder.clearContext(); }
        chain.doFilter(request, response);
    }
}
