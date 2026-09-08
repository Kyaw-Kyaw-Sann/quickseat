package com.quickseat.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "QuickSeat API",
        version = "v1",
        description = "QuickSeat multi-cinema ticket-booking backend API"
))
public class OpenApiConfig {
}
