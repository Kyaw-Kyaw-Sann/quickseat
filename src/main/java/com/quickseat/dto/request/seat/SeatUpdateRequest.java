package com.quickseat.dto.request.seat;

import com.quickseat.entity.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeatUpdateRequest(
        @NotBlank @Size(max = 10) String rowName,
        @NotNull @Positive Integer seatNumber,
        @NotNull SeatType seatType
) { }
