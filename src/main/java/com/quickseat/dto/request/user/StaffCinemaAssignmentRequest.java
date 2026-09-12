package com.quickseat.dto.request.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StaffCinemaAssignmentRequest(
        @NotNull @Positive Long cinemaId
) { }
