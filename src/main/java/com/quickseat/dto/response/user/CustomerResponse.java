package com.quickseat.dto.response.user;

import com.quickseat.entity.enums.AuthProvider;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        AuthProvider provider,
        boolean emailVerified,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) { }
