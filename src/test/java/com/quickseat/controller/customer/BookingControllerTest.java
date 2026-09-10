package com.quickseat.controller.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.booking.BookingCategory;
import com.quickseat.dto.response.booking.BookingResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.service.booking.BookingManagementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookingControllerTest {
    private BookingManagementService bookingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingManagementService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService)).build();
    }

    @Test
    void forwardsStatusCategoryDateAndPaginationFilters() throws Exception {
        when(bookingService.list(any(), any(), any(), eq(1), eq(5)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/customer/bookings")
                        .param("status", "PENDING")
                        .param("category", "UPCOMING")
                        .param("date", "2026-09-10")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).list(BookingStatus.PENDING, BookingCategory.UPCOMING,
                LocalDate.of(2026, 9, 10), 1, 5);
    }

    @Test
    void returnsBookingDetailAndCancellation() throws Exception {
        when(bookingService.get("QS-BOOKING")).thenReturn(response(BookingStatus.PENDING));
        when(bookingService.cancel("QS-BOOKING")).thenReturn(response(BookingStatus.CANCELLED));

        mockMvc.perform(get("/customer/bookings/QS-BOOKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        mockMvc.perform(patch("/customer/bookings/QS-BOOKING/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void rejectsInvalidStatusValue() throws Exception {
        mockMvc.perform(get("/customer/bookings").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    private BookingResponse response(BookingStatus status) {
        Instant start = Instant.parse("2026-09-10T12:00:00Z");
        return new BookingResponse(5L, "QS-BOOKING", status, BookingCategory.UPCOMING,
                new BigDecimal("5000.00"), start.minusSeconds(300), 0, null,
                status == BookingStatus.CANCELLED ? Instant.parse("2026-09-10T10:00:00Z") : null,
                Instant.parse("2026-09-10T09:59:00Z"), 4L, "Booking Movie", "Downtown Cinema",
                "Screen 1", start, start.plusSeconds(7_200), List.of());
    }
}
