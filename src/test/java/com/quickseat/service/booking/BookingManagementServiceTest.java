package com.quickseat.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.booking.BookingCategory;
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
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.repository.TicketRepository;
import com.quickseat.security.CurrentUserService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BookingManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock SeatHoldService seatHoldService;

    private BookingManagementService service;
    private User customer;
    private Booking booking;
    private BookingSeat snapshot;
    private ShowtimeSeat inventory;

    @BeforeEach
    void setUp() {
        service = new BookingManagementService(bookingRepository, bookingSeatRepository,
                showtimeSeatRepository, ticketRepository, currentUserService, seatHoldService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        customer = new User();
        customer.setId(50L);
        customer.setEmailVerified(true);

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("Downtown Cinema");
        cinema.setActive(true);
        Screen screen = new Screen();
        screen.setId(2L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        screen.setActive(true);
        Movie movie = new Movie();
        movie.setId(3L);
        movie.setTitle("Booking Movie");
        movie.setActive(true);
        Showtime showtime = new Showtime();
        showtime.setId(4L);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(NOW.plusSeconds(7_200));
        showtime.setEndTime(NOW.plusSeconds(14_400));

        booking = new Booking();
        booking.setId(5L);
        booking.setBookingReference("QS-BOOKING");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setExpiresAt(NOW.plusSeconds(300));
        booking.setCreatedAt(NOW.minusSeconds(60));

        Seat seat = new Seat();
        seat.setId(6L);
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
        inventory.setId(7L);
        inventory.setShowtime(showtime);
        inventory.setSeat(seat);
        inventory.setStatus(SeatInventoryStatus.HELD);
        inventory.setHeldByBooking(booking);
        inventory.setHoldExpiresAt(booking.getExpiresAt());
    }

    @Test
    void listsOnlyCurrentCustomerBookingsWithStatusCategoryDateAndPagination() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingSeatRepository.findByBookingIdsWithSeats(List.of(5L))).thenReturn(List.of(snapshot));

        var result = service.list(BookingStatus.PENDING, BookingCategory.UPCOMING,
                LocalDate.of(2026, 9, 10), 1, 5);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.content().getFirst().category()).isEqualTo(BookingCategory.UPCOMING);
        assertThat(result.content().getFirst().seats()).hasSize(1);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        verify(seatHoldService).releaseExpiredHolds();
    }

    @Test
    void classifiesBookingAsPastWhenShowtimeHasStarted() {
        booking.getShowtime().setStartTime(NOW);
        mockOwnedBooking();

        var result = service.get("QS-BOOKING");

        assertThat(result.category()).isEqualTo(BookingCategory.PAST);
    }

    @Test
    void cannotAccessAnotherCustomersBooking() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-OTHER", 50L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("QS-OTHER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found");
    }

    @Test
    void pendingCancellationReleasesHeldSeatsAndStoresCancelledTime() {
        mockOwnedBooking();
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(5L)).thenReturn(List.of(inventory));

        var result = service.cancel("QS-BOOKING");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.cancelledAt()).isEqualTo(NOW);
        verify(seatHoldService).releaseInventorySeat(inventory);
    }

    @Test
    void confirmedCancellationBeforeShowtimeReleasesBookedSeats() {
        booking.setStatus(BookingStatus.CONFIRMED);
        inventory.setStatus(SeatInventoryStatus.BOOKED);
        inventory.setHeldByBooking(null);
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setStatus(TicketStatus.ACTIVE);
        mockOwnedBooking();
        when(showtimeSeatRepository.findBookingSeatsForUpdate(4L, List.of(6L)))
                .thenReturn(List.of(inventory));
        when(ticketRepository.findByBookingId(5L)).thenReturn(Optional.of(ticket));

        var result = service.cancel("QS-BOOKING");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(seatHoldService).releaseInventorySeat(inventory);
    }

    @Test
    void confirmedCancellationAfterShowtimeStartsIsRejected() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getShowtime().setStartTime(NOW);
        mockOwnedBooking();

        assertThatThrownBy(() -> service.cancel("QS-BOOKING"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after the showtime starts");
        verify(showtimeSeatRepository, never()).findBookingSeatsForUpdate(any(), anyList());
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"EXPIRED", "CANCELLED", "USED"})
    void invalidCancellationTransitionsAreRejected(BookingStatus status) {
        booking.setStatus(status);
        mockOwnedBooking();

        assertThatThrownBy(() -> service.cancel("QS-BOOKING"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(status.name());
    }

    @Test
    void stalePendingBookingIsExpiredBeforeDetailIsReturned() {
        booking.setExpiresAt(NOW.minusSeconds(1));
        mockOwnedBooking();
        when(seatHoldService.expirePendingBookingIfNeeded(booking, NOW)).thenAnswer(invocation -> {
            booking.setStatus(BookingStatus.EXPIRED);
            return true;
        });

        var result = service.get("QS-BOOKING");

        assertThat(result.status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(result.remainingSeconds()).isZero();
        verify(seatHoldService).expirePendingBookingIfNeeded(booking, NOW);
    }

    private void mockOwnedBooking() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-BOOKING", 50L))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findByBookingIdWithSeats(5L)).thenReturn(List.of(snapshot));
    }
}
