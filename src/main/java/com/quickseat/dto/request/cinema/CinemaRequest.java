package com.quickseat.dto.request.cinema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CinemaRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank String address,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 30) String phone,
        String imageUrl
) { }
