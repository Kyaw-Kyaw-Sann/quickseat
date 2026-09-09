package com.quickseat.dto.request.movie;

import com.quickseat.entity.enums.MovieStatus;
import jakarta.validation.constraints.NotNull;

public record MovieStatusRequest(@NotNull MovieStatus status) { }
