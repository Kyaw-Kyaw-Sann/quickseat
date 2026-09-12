package com.quickseat.dto.request.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketValidationRequest(
        @NotBlank(message = "Ticket token is required")
        @Size(max = 255, message = "Ticket token must not exceed 255 characters")
        String ticketToken
) { }
