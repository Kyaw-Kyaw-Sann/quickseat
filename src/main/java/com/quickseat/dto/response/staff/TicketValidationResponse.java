package com.quickseat.dto.response.staff;

public record TicketValidationResponse(
        String result,
        StaffTicketResponse ticket
) { }
