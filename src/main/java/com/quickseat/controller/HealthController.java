package com.quickseat.controller;

import com.quickseat.dto.common.ApiResponse;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health endpoint requested");
        Map<String, Object> data = Map.of("status", "UP", "timestamp", Instant.now());
        return ResponseEntity.ok(ApiResponse.success("QuickSeat API is running", data));
    }
}
