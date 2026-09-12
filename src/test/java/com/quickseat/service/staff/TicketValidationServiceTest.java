package com.quickseat.service.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.TicketRepository;
import com.quickseat.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@ExtendWith(MockitoExtension.class)
class TicketValidationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");

    @Mock TicketRepository ticketRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock CurrentUserService currentUserService;

    private TicketValidationService service;
    private User staff;
    private Ticket ticket;
    private Booking booking;
    private BookingSeat bookingSeat;

    @BeforeEach
    void setUp() {
        service = new TicketValidationService(ticketRepository, bookingSeatRepository,
                currentUserService, Clock.fixed(NOW, ZoneOffset.UTC));
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("Cinema One");
        cinema.setActive(true);
        staff = new User();
        staff.setId(2L);
        staff.setCinema(cinema);

        Screen screen = new Screen();
        screen.setId(3L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("Staff Movie");
        Showtime showtime = new Showtime();
        showtime.setId(4L);
        showtime.setScreen(screen);
        showtime.setMovie(movie);
        showtime.setStartTime(NOW.plusSeconds(3600));

        User customer = new User();
        customer.setId(5L);
        customer.setName("Customer");
        booking = new Booking();
        booking.setId(6L);
        booking.setBookingReference("QS-STAFF");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("6000.00"));

        ticket = new Ticket();
        ticket.setId(7L);
        ticket.setTicketToken("ticket-token");
        ticket.setBooking(booking);
        ticket.setStatus(TicketStatus.ACTIVE);

        Seat seat = new Seat();
        seat.setId(8L);
        seat.setRowName("A");
        seat.setSeatNumber(3);
        bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setSeat(seat);
        bookingSeat.setSeatType(SeatType.NORMAL);
        bookingSeat.setUnitPrice(new BigDecimal("6000.00"));
    }

    @Test
    void validTicketMarksTicketAndBookingUsed() {
        mockValidation();

        var result = service.validate("ticket-token");

        assertThat(result.result()).isEqualTo("VALID");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getUsedAt()).isEqualTo(NOW);
        assertThat(ticket.getVerifiedBy()).isSameAs(staff);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.USED);
        assertThat(result.ticket().verifiedByStaffId()).isEqualTo(2L);
        verify(ticketRepository).flush();
    }

    @Test
    void repeatedTicketScanIsRejected() {
        ticket.setStatus(TicketStatus.USED);
        mockLockedTicket();

        assertThatThrownBy(() -> service.validate("ticket-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been used");
        verify(ticketRepository, never()).flush();
    }

    @Test
    void cancelledTicketIsRejected() {
        ticket.setStatus(TicketStatus.CANCELLED);
        mockLockedTicket();

        assertThatThrownBy(() -> service.validate("ticket-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void ticketWithNonConfirmedBookingIsRejected() {
        booking.setStatus(BookingStatus.CANCELLED);
        mockLockedTicket();

        assertThatThrownBy(() -> service.validate("ticket-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not confirmed");
    }

    @Test
    void ticketFromAnotherCinemaIsRejected() {
        Cinema otherCinema = new Cinema();
        otherCinema.setId(99L);
        ticket.getBooking().getShowtime().getScreen().setCinema(otherCinema);
        mockLockedTicket();

        assertThatThrownBy(() -> service.validate("ticket-token"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another cinema");
        verify(ticketRepository, never()).flush();
    }

    @Test
    void invalidTicketTokenIsRejected() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(ticketRepository.findByTicketTokenForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getsTicketDetailsOnlyForAssignedCinema() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(ticketRepository.findByTicketTokenWithDetails("ticket-token")).thenReturn(Optional.of(ticket));
        when(bookingSeatRepository.findByBookingIdWithSeats(6L)).thenReturn(List.of(bookingSeat));

        var result = service.getTicket("ticket-token");

        assertThat(result.bookingReference()).isEqualTo("QS-STAFF");
        assertThat(result.seats()).hasSize(1);
    }

    @Test
    void transactionRollsBackWhenFinalFlushFails() {
        mockLockedTicket();
        RuntimeException databaseFailure = new RuntimeException("database write failed");
        org.mockito.Mockito.doThrow(databaseFailure).when(ticketRepository).flush();
        PlatformTransactionManager transactionManager = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(interceptor);
        TicketValidationService transactionalService = (TicketValidationService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.validate("ticket-token")).isSameAs(databaseFailure);

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    private void mockValidation() {
        mockLockedTicket();
        when(bookingSeatRepository.findByBookingIdWithSeats(6L)).thenReturn(List.of(bookingSeat));
    }

    private void mockLockedTicket() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(ticketRepository.findByTicketTokenForUpdate("ticket-token")).thenReturn(Optional.of(ticket));
    }
}
