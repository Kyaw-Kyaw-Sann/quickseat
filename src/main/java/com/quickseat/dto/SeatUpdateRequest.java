package com.quickseat.dto;

import com.quickseat.entity.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeatUpdateRequest(
        @NotBlank @Size(max = 10) String rowName,
        @NotNull @Positive Integer seatNumber,
        @NotNull SeatType seatType
) { }
