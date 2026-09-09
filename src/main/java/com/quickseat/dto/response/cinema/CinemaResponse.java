package com.quickseat.dto.response.cinema;

import java.time.Instant;

public record CinemaResponse(Long id, String name, String address, String city, String phone,
                             String imageUrl, boolean active, Instant createdAt, Instant updatedAt) { }
