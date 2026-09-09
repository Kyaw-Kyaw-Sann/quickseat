package com.quickseat.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickseat.dto.common.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsUsefulDetailsForMissingRequestParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/verify-email");

        var response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("token", "String"), request);

        ApiErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("MISSING_PARAMETER");
        assertThat(body.message()).isEqualTo("Missing required parameter: token");
        assertThat(body.path()).isEqualTo("/api/v1/auth/verify-email");
    }

    @Test
    void doesNotExposeUnexpectedExceptionDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

        var response = handler.handleUnexpected(new RuntimeException("database password"), request);

        ApiErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("An unexpected server error occurred");
        assertThat(body.message()).doesNotContain("password");
    }
}
