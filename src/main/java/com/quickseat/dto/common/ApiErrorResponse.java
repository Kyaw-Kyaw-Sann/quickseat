package com.quickseat.dto.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(boolean success, String timestamp, int status, String error,
                               String message, String path, Map<String, String> fieldErrors) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(false, Instant.now().toString(), status, error, message, path, Map.of());
    }

    public static ApiErrorResponse validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(false, Instant.now().toString(), 400, "VALIDATION_FAILED", message, path, fieldErrors);
    }
}
