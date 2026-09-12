package com.quickseat.controller.staff;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.response.staff.StaffTicketResponse;
import com.quickseat.dto.response.staff.TicketValidationResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.service.staff.TicketValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffTicketControllerTest {
    private TicketValidationService ticketService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ticketService = mock(TicketValidationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StaffTicketController(ticketService)).build();
    }

    @Test
    void returnsTicketDetails() throws Exception {
        when(ticketService.getTicket("ticket-token")).thenReturn(ticket());

        mockMvc.perform(get("/staff/tickets/ticket-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value("QS-STAFF"));
    }

    @Test
    void validatesTicketToken() throws Exception {
        when(ticketService.validate("ticket-token"))
                .thenReturn(new TicketValidationResponse("VALID", ticket()));

        mockMvc.perform(post("/staff/tickets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketToken":"ticket-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("VALID"))
                .andExpect(jsonPath("$.data.ticket.ticketStatus").value("USED"));
    }

    @Test
    void rejectsBlankTicketToken() throws Exception {
        mockMvc.perform(post("/staff/tickets/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketToken":" "}
                                """))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).validate(anyString());
    }

    private StaffTicketResponse ticket() {
        return new StaffTicketResponse(7L, "ticket-token", TicketStatus.USED, "QS-STAFF",
                BookingStatus.USED, new BigDecimal("6000.00"), 5L, "Customer", "Staff Movie",
                "Cinema One", "Screen 1", Instant.parse("2026-09-11T10:00:00Z"), List.of(),
                Instant.parse("2026-09-10T12:00:00Z"), 2L);
    }
}
