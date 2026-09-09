package com.quickseat.dto.response.screen;

import java.time.Instant;

public record ScreenResponse(Long id, Long cinemaId, String name, boolean active,
                             Instant createdAt, Instant updatedAt) { }
