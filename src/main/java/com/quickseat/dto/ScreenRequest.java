package com.quickseat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScreenRequest(@NotBlank @Size(max = 100) String name) { }
