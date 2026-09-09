package com.quickseat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeatRowRequest(
        @NotBlank @Size(max = 10) String rowName,
        @Min(0) @Max(100) int normalSeats,
        @Min(0) @Max(100) int coupleSeats
) { }
