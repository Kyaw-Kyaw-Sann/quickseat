package com.quickseat.controller.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.request.payment.MockPaymentRequest;
import com.quickseat.dto.response.payment.PaymentResponse;
import com.quickseat.dto.response.payment.PaymentSummaryResponse;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import com.quickseat.service.payment.MockPaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentControllerTest {
    private MockPaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(MockPaymentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService)).build();
    }

    @Test
    void returnsPaymentSummary() throws Exception {
        when(paymentService.getSummary("QS-PAYMENT")).thenReturn(new PaymentSummaryResponse(
                "QS-PAYMENT", BookingStatus.PENDING, new BigDecimal("5000.00"),
                Instant.parse("2026-09-10T10:05:00Z"), 300, null, null, true));

        mockMvc.perform(get("/customer/bookings/QS-PAYMENT/payment-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.canPay").value(true));
    }

    @Test
    void processesSuccessfulMockPayment() throws Exception {
        when(paymentService.process(org.mockito.ArgumentMatchers.eq("QS-PAYMENT"),
                any(MockPaymentRequest.class))).thenReturn(response(PaymentStatus.SUCCESS));

        mockMvc.perform(post("/customer/bookings/QS-PAYMENT/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"successful":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));
    }

    @Test
    void rejectsMissingPaymentResult() throws Exception {
        mockMvc.perform(post("/customer/bookings/QS-PAYMENT/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(paymentService, org.mockito.Mockito.never()).process(any(), any());
    }

    private PaymentResponse response(PaymentStatus status) {
        return new PaymentResponse(8L, "PAY-TEST", status, new BigDecimal("5000.00"),
                Instant.parse("2026-09-10T10:00:00Z"), "QS-PAYMENT", BookingStatus.CONFIRMED,
                Instant.parse("2026-09-10T10:00:00Z"), false);
    }
}
