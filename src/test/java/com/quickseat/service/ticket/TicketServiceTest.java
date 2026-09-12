package com.quickseat.service.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.response.ticket.TicketPdfDocument;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Payment;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.PaymentStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.PaymentRepository;
import com.quickseat.repository.TicketRepository;
import com.quickseat.security.CurrentUserService;
import com.quickseat.service.shared.EmailService;
import com.quickseat.service.shared.QrCodeService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock QrCodeService qrCodeService;
    @Mock TicketPdfService ticketPdfService;
    @Mock EmailService emailService;
    @Mock SecureRandom secureRandom;

    private TicketService service;
    private User customer;
    private Booking booking;
    private BookingSeat bookingSeat;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new TicketService(bookingRepository, bookingSeatRepository, paymentRepository,
                ticketRepository, currentUserService, qrCodeService, ticketPdfService, emailService, secureRandom,
                "https://api.quickseat.test/api/v1/staff/tickets/validate");

        customer = new User();
        customer.setId(10L);
        customer.setEmail("customer@example.com");
        customer.setEmailVerified(true);

        Cinema cinema = new Cinema();
        cinema.setName("QuickSeat Cinema");
        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(Instant.parse("2026-09-11T10:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-09-11T12:00:00Z"));

        booking = new Booking();
        booking.setId(20L);
        booking.setBookingReference("QS-TICKET");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("8000.00"));

        Seat seat = new Seat();
        seat.setId(30L);
        seat.setRowName("A");
        seat.setSeatNumber(5);
        bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setSeat(seat);
        bookingSeat.setSeatType(SeatType.NORMAL);
        bookingSeat.setUnitPrice(new BigDecimal("8000.00"));

        payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(new BigDecimal("8000.00"));
        payment.setPaidAt(Instant.parse("2026-09-10T10:00:00Z"));
    }

    @Test
    void generatesTicketAndSendsConfirmationEmailWithQrAttachment() {
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(20L)).thenReturn(Optional.of(payment));
        when(bookingSeatRepository.findByBookingIdWithSeats(20L)).thenReturn(List.of(bookingSeat));
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(40L);
            ticket.setCreatedAt(Instant.parse("2026-09-10T10:01:00Z"));
            return ticket;
        });
        when(qrCodeService.generatePng(anyString())).thenReturn(new byte[] {1, 2, 3});

        var result = service.generate("QS-TICKET");

        assertThat(result.status()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(result.ticketToken()).isNotBlank();
        assertThat(result.qrImageUrl()).endsWith("/qr");
        assertThat(result.alreadyGenerated()).isFalse();
        verify(emailService).sendWithAttachment(org.mockito.ArgumentMatchers.eq("customer@example.com"),
                anyString(), org.mockito.ArgumentMatchers.contains("QS-TICKET"), anyString(),
                any(byte[].class), org.mockito.ArgumentMatchers.eq("image/png"));
    }

    @Test
    void repeatedGenerationReturnsExistingTicketWithoutSendingAnotherEmail() {
        Ticket ticket = ticket();
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(20L)).thenReturn(Optional.of(payment));
        when(ticketRepository.findByBookingId(20L)).thenReturn(Optional.of(ticket));
        when(bookingSeatRepository.findByBookingIdWithSeats(20L)).thenReturn(List.of(bookingSeat));

        var result = service.generate("QS-TICKET");

        assertThat(result.alreadyGenerated()).isTrue();
        assertThat(result.ticketToken()).isEqualTo("ticket-token");
        verify(ticketRepository, never()).saveAndFlush(any());
        verify(emailService, never()).sendWithAttachment(anyString(), anyString(), anyString(),
                anyString(), any(), anyString());
    }

    @Test
    void rejectsTicketForBookingThatIsNotConfirmed() {
        booking.setStatus(BookingStatus.PENDING);
        mockOwnedBooking();

        assertThatThrownBy(() -> service.generate("QS-TICKET"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("confirmed");
        verify(ticketRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTicketWithoutConsistentSuccessfulPayment() {
        payment.setStatus(PaymentStatus.FAILED);
        mockOwnedBooking();
        when(paymentRepository.findByBookingId(20L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.generate("QS-TICKET"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inconsistent");
    }

    @Test
    void anotherCustomerCannotAccessBookingTicket() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-OTHER", 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByBooking("QS-OTHER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retrievesTicketForOwnedBooking() {
        mockOwnedBooking();
        when(ticketRepository.findByBookingId(20L)).thenReturn(Optional.of(ticket()));
        when(bookingSeatRepository.findByBookingIdWithSeats(20L)).thenReturn(List.of(bookingSeat));

        var result = service.getByBooking("QS-TICKET");

        assertThat(result.bookingReference()).isEqualTo("QS-TICKET");
        assertThat(result.seats()).hasSize(1);
    }

    @Test
    void returnsPngForOwnedActiveTicket() {
        Ticket ticket = ticket();
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(ticketRepository.findCustomerTicketByToken("ticket-token", 10L)).thenReturn(Optional.of(ticket));
        when(qrCodeService.generatePng(anyString())).thenReturn(new byte[] {9, 8, 7});

        byte[] result = service.getQrImage("ticket-token");

        assertThat(result).containsExactly(9, 8, 7);
        verify(qrCodeService).generatePng(
                "https://api.quickseat.test/api/v1/staff/tickets/validate?token=ticket-token");
    }

    @Test
    void rejectsQrAccessForAnotherCustomer() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(ticketRepository.findCustomerTicketByToken("other-token", 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQrImage("other-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnsPdfForOwnedActiveTicket() {
        Ticket ticket = ticket();
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(ticketRepository.findCustomerTicketByToken("ticket-token", 10L)).thenReturn(Optional.of(ticket));
        when(bookingSeatRepository.findByBookingIdWithSeats(20L)).thenReturn(List.of(bookingSeat));
        when(qrCodeService.generatePng(anyString())).thenReturn(new byte[] {1, 2, 3});
        when(ticketPdfService.generate(ticket, List.of(bookingSeat), new byte[] {1, 2, 3}))
                .thenReturn(new byte[] {4, 5, 6});

        TicketPdfDocument result = service.getPdf("ticket-token");

        assertThat(result.filename()).isEqualTo("quickseat-ticket-QS-TICKET.pdf");
        assertThat(result.content()).containsExactly(4, 5, 6);
        verify(qrCodeService).generatePng(
                "https://api.quickseat.test/api/v1/staff/tickets/validate?token=ticket-token");
        verify(ticketPdfService).generate(ticket, List.of(bookingSeat), new byte[] {1, 2, 3});
    }

    @Test
    void anotherCustomerCannotDownloadPdf() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(ticketRepository.findCustomerTicketByToken("other-token", 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPdf("other-token"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketPdfService, never()).generate(any(), any(), any());
    }

    @Test
    void usedAndCancelledTicketsCannotDownloadPdf() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        Ticket ticket = ticket();
        when(ticketRepository.findCustomerTicketByToken("ticket-token", 10L)).thenReturn(Optional.of(ticket));

        ticket.setStatus(TicketStatus.USED);
        assertThatThrownBy(() -> service.getPdf("ticket-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active ticket");

        ticket.setStatus(TicketStatus.CANCELLED);
        assertThatThrownBy(() -> service.getPdf("ticket-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active ticket");
        verify(ticketPdfService, never()).generate(any(), any(), any());
    }

    private void mockOwnedBooking() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-TICKET", 10L)).thenReturn(Optional.of(booking));
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(40L);
        ticket.setBooking(booking);
        ticket.setTicketToken("ticket-token");
        ticket.setQrImageUrl("/api/v1/customer/tickets/ticket-token/qr");
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setCreatedAt(Instant.parse("2026-09-10T10:01:00Z"));
        return ticket;
    }
}
