package com.quickseat.dto;

import java.util.Map;

public record ValidationErrorResponse(boolean success, String message, Map<String, String> errors) {

    public static ValidationErrorResponse of(Map<String, String> errors) {
        return new ValidationErrorResponse(false, "Validation failed", errors);
    }
}
