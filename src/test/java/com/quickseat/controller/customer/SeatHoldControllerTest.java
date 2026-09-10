package com.quickseat.controller.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.request.booking.SeatHoldRequest;
import com.quickseat.dto.response.booking.HeldSeatResponse;
import com.quickseat.dto.response.booking.SeatHoldResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.service.booking.SeatHoldService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SeatHoldControllerTest {
    private SeatHoldService seatHoldService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        seatHoldService = mock(SeatHoldService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SeatHoldController(seatHoldService)).build();
    }

    @Test
    void createsSeatHold() throws Exception {
        when(seatHoldService.hold(any(SeatHoldRequest.class))).thenReturn(response(BookingStatus.PENDING));

        mockMvc.perform(post("/customer/seat-holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"showtimeId":4,"showtimeSeatIds":[10]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("QS-TEST"))
                .andExpect(jsonPath("$.data.remainingSeconds").value(300));
    }

    @Test
    void retrievesAndReleasesOwnSeatHold() throws Exception {
        when(seatHoldService.get("QS-TEST")).thenReturn(response(BookingStatus.PENDING));
        when(seatHoldService.release("QS-TEST")).thenReturn(response(BookingStatus.CANCELLED));

        mockMvc.perform(get("/customer/seat-holds/QS-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        mockMvc.perform(delete("/customer/seat-holds/QS-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(seatHoldService).get("QS-TEST");
        verify(seatHoldService).release("QS-TEST");
    }

    @Test
    void rejectsEmptySeatSelectionAtApiBoundary() throws Exception {
        mockMvc.perform(post("/customer/seat-holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"showtimeId":4,"showtimeSeatIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    private SeatHoldResponse response(BookingStatus status) {
        HeldSeatResponse seat = new HeldSeatResponse(10L, 20L, "A", 1, SeatType.NORMAL,
                new BigDecimal("5000.00"));
        return new SeatHoldResponse(100L, "QS-TEST", status, new BigDecimal("5000.00"),
                Instant.parse("2026-09-10T10:05:00Z"), status == BookingStatus.PENDING ? 300 : 0,
                List.of(seat));
    }
}
