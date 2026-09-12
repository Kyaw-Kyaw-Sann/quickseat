package com.quickseat.dto.response.ticket;

public record TicketPdfDocument(
        String filename,
        byte[] content
) { }
