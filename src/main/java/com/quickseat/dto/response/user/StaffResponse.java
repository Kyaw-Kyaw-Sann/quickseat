package com.quickseat.dto.response.user;

import com.quickseat.entity.enums.AuthProvider;
import java.time.Instant;

public record StaffResponse(
        Long id,
        String name,
        String email,
        String phone,
        AuthProvider provider,
        boolean active,
        Long cinemaId,
        String cinemaName,
        Instant createdAt,
        Instant updatedAt
) { }
