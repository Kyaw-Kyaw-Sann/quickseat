package com.quickseat.controller.customer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.response.ticket.TicketPdfDocument;
import com.quickseat.dto.response.ticket.TicketResponse;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.service.ticket.TicketService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TicketControllerTest {
    private TicketService ticketService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ticketService = mock(TicketService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(ticketService)).build();
    }

    @Test
    void generatesTicket() throws Exception {
        when(ticketService.generate("QS-TICKET")).thenReturn(response(false));

        mockMvc.perform(post("/customer/bookings/QS-TICKET/ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticket generated successfully"))
                .andExpect(jsonPath("$.data.ticketToken").value("ticket-token"));
    }

    @Test
    void getsTicketByBookingReference() throws Exception {
        when(ticketService.getByBooking("QS-TICKET")).thenReturn(response(false));

        mockMvc.perform(get("/customer/bookings/QS-TICKET/ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void returnsDownloadablePng() throws Exception {
        when(ticketService.getQrImage("ticket-token")).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/customer/tickets/ticket-token/qr").param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quickseat-ticket.png\""))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
        verify(ticketService).getQrImage("ticket-token");
    }

    @Test
    void returnsDownloadablePdfWithBookingReferenceFilename() throws Exception {
        byte[] pdf = "%PDF-1.7 test".getBytes();
        when(ticketService.getPdf("ticket-token"))
                .thenReturn(new TicketPdfDocument("quickseat-ticket-QS-TICKET.pdf", pdf));

        mockMvc.perform(get("/customer/tickets/ticket-token/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quickseat-ticket-QS-TICKET.pdf\""))
                .andExpect(content().bytes(pdf));
        verify(ticketService).getPdf("ticket-token");
    }

    private TicketResponse response(boolean alreadyGenerated) {
        return new TicketResponse(40L, "ticket-token", TicketStatus.ACTIVE,
                "/api/v1/customer/tickets/ticket-token/qr", "QS-TICKET",
                new BigDecimal("8000.00"), "Test Movie", "QuickSeat Cinema", "Screen 1",
                Instant.parse("2026-09-11T10:00:00Z"), Instant.parse("2026-09-11T12:00:00Z"),
                List.of(), Instant.parse("2026-09-10T10:01:00Z"), alreadyGenerated);
    }
}
