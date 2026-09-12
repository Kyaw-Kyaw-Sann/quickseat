package com.quickseat.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.Ticket;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.TicketStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SeatHoldService seatHoldService;

    private AdminBookingService service;
    private Booking booking;
    private BookingSeat snapshot;
    private ShowtimeSeat inventory;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        service = new AdminBookingService(bookingRepository, bookingSeatRepository,
                showtimeSeatRepository, ticketRepository, seatHoldService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("Cinema One");
        Screen screen = new Screen();
        screen.setId(2L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setId(3L);
        movie.setTitle("Admin Movie");
        Showtime showtime = new Showtime();
        showtime.setId(4L);
        showtime.setScreen(screen);
        showtime.setMovie(movie);
        showtime.setStartTime(NOW.plusSeconds(7200));
        showtime.setEndTime(NOW.plusSeconds(14400));
        User customer = new User();
        customer.setId(5L);
        customer.setName("Customer");
        customer.setEmail("customer@example.com");
        booking = new Booking();
        booking.setId(6L);
        booking.setBookingReference("QS-ADMIN");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setExpiresAt(NOW.plusSeconds(300));

        Seat seat = new Seat();
        seat.setId(7L);
        seat.setRowName("A");
        seat.setSeatNumber(1);
        seat.setSeatType(SeatType.NORMAL);
        seat.setActive(true);
        snapshot = new BookingSeat();
        snapshot.setBooking(booking);
        snapshot.setSeat(seat);
        snapshot.setSeatType(SeatType.NORMAL);
        snapshot.setUnitPrice(new BigDecimal("5000.00"));
        inventory = new ShowtimeSeat();
        inventory.setShowtime(showtime);
        inventory.setSeat(seat);
        inventory.setStatus(SeatInventoryStatus.HELD);
        inventory.setHeldByBooking(booking);
        ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setStatus(TicketStatus.ACTIVE);
    }

    @Test
    void listsSystemBookingsWithAllFiltersAndSearch() {
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingSeatRepository.findByBookingIdsWithSeats(List.of(6L))).thenReturn(List.of(snapshot));
        when(ticketRepository.findByBookingIdIn(List.of(6L))).thenReturn(List.of());

        var result = service.list(BookingStatus.PENDING, 1L, 3L, 4L,
                LocalDate.of(2026, 9, 10), "customer@example.com", 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().customerEmail()).isEqualTo("customer@example.com");
        verify(seatHoldService).releaseExpiredHolds();
    }

    @Test
    void returnsBookingDetailWithTicketStatus() {
        booking.setStatus(BookingStatus.CONFIRMED);
        mockBookingDetail(false);
        when(ticketRepository.findByBookingId(6L)).thenReturn(Optional.of(ticket));

        var result = service.get("QS-ADMIN");

        assertThat(result.ticketStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(result.seats()).hasSize(1);
    }

    @Test
    void pendingCancellationReleasesHeldSeats() {
        mockBookingDetail(true);
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(6L)).thenReturn(List.of(inventory));

        var result = service.cancel("QS-ADMIN");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.cancelledAt()).isEqualTo(NOW);
        verify(seatHoldService).releaseInventorySeat(inventory);
        verify(bookingRepository).flush();
    }

    @Test
    void confirmedCancellationReleasesSeatsAndCancelsTicket() {
        booking.setStatus(BookingStatus.CONFIRMED);
        inventory.setStatus(SeatInventoryStatus.BOOKED);
        inventory.setHeldByBooking(null);
        mockBookingDetail(true);
        when(ticketRepository.findByBookingId(6L)).thenReturn(Optional.of(ticket));
        when(showtimeSeatRepository.findBookingSeatsForUpdate(4L, List.of(7L)))
                .thenReturn(List.of(inventory));

        var result = service.cancel("QS-ADMIN");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(seatHoldService).releaseInventorySeat(inventory);
    }

    @Test
    void confirmedCancellationAfterShowtimeStartsIsRejected() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getShowtime().setStartTime(NOW);
        mockBookingDetail(true);

        assertThatThrownBy(() -> service.cancel("QS-ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after the showtime starts");
        verify(showtimeSeatRepository, never()).findBookingSeatsForUpdate(any(), anyList());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"EXPIRED", "CANCELLED", "USED"})
    void invalidCancellationStatusesAreRejected(BookingStatus status) {
        booking.setStatus(status);
        mockBookingDetail(true);

        assertThatThrownBy(() -> service.cancel("QS-ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(status.name());
    }

    @Test
    void stalePendingBookingIsExpiredBeforeCancellation() {
        mockBookingDetail(true);
        when(seatHoldService.expirePendingBookingIfNeeded(booking, NOW)).thenAnswer(invocation -> {
            booking.setStatus(BookingStatus.EXPIRED);
            return true;
        });

        assertThatThrownBy(() -> service.cancel("QS-ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EXPIRED");
    }

    @Test
    void transactionRollbackPreventsPartialCancellation() {
        mockBookingDetail(true);
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(6L)).thenReturn(List.of(inventory));
        RuntimeException databaseFailure = new RuntimeException("database write failed");
        org.mockito.Mockito.doThrow(databaseFailure).when(bookingRepository).flush();
        PlatformTransactionManager transactionManager = org.mockito.Mockito.mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(service);
        proxyFactory.addAdvice(interceptor);
        AdminBookingService transactionalService = (AdminBookingService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.cancel("QS-ADMIN")).isSameAs(databaseFailure);

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    private void mockBookingDetail(boolean locked) {
        if (locked) {
            when(bookingRepository.findByBookingReferenceForUpdate("QS-ADMIN"))
                    .thenReturn(Optional.of(booking));
        } else {
            when(bookingRepository.findByBookingReference("QS-ADMIN"))
                    .thenReturn(Optional.of(booking));
        }
        when(bookingSeatRepository.findByBookingIdWithSeats(6L)).thenReturn(List.of(snapshot));
    }
}
