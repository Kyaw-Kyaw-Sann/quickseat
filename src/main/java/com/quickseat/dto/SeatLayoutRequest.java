package com.quickseat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatLayoutRequest(@NotEmpty @Size(max = 50) List<@Valid SeatRowRequest> rows) { }
