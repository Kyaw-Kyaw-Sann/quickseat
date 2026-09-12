package com.quickseat.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    void configuresAllConfiguredFrontendOriginsAndPdfDownloadHeader() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource(
                "http://localhost:3000, https://quickseat.example.com");

        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "https://quickseat.example.com");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(configuration.getExposedHeaders()).contains("Content-Disposition");
    }
}
