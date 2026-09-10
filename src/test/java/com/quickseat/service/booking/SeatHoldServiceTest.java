package com.quickseat.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.booking.SeatHoldRequest;
import com.quickseat.entity.Booking;
import com.quickseat.entity.BookingSeat;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock ShowtimeRepository showtimeRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;
    @Mock CurrentUserService currentUserService;

    private SeatHoldService service;
    private User customer;
    private Showtime showtime;
    private ShowtimeSeat normalInventory;
    private ShowtimeSeat coupleInventory;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new SeatHoldService(bookingRepository, bookingSeatRepository, showtimeRepository,
                showtimeSeatRepository, currentUserService, clock, 5);

        customer = new User();
        customer.setId(50L);
        customer.setEmailVerified(true);
        customer.setActive(true);

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setActive(true);
        Screen screen = new Screen();
        screen.setId(2L);
        screen.setActive(true);
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setId(3L);
        movie.setActive(true);

        showtime = new Showtime();
        showtime.setId(4L);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(NOW.plusSeconds(7_200));
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));

        normalInventory = inventory(10L, "A", 1, SeatType.NORMAL, true);
        coupleInventory = inventory(11L, "A", 2, SeatType.COUPLE, true);
    }

    @Test
    void createsPendingBookingWithPriceSnapshotsTotalAndFiveMinuteExpiry() {
        mockValidHold(List.of(normalInventory, coupleInventory));

        var response = service.hold(new SeatHoldRequest(4L, List.of(10L, 11L)));

        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("14000.00");
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(response.remainingSeconds()).isEqualTo(300);
        assertThat(response.selectedSeats()).extracting(seat -> seat.unitPrice())
                .containsExactly(new BigDecimal("5000.00"), new BigDecimal("9000.00"));
        assertThat(normalInventory.getStatus()).isEqualTo(SeatInventoryStatus.HELD);
        assertThat(normalInventory.getHeldByBooking()).isNotNull();
        assertThat(normalInventory.getHoldExpiresAt()).isEqualTo(NOW.plusSeconds(300));

        ArgumentCaptor<List<BookingSeat>> snapshots = ArgumentCaptor.forClass(List.class);
        verify(bookingSeatRepository).saveAll(snapshots.capture());
        assertThat(snapshots.getValue()).extracting(BookingSeat::getSeatType)
                .containsExactly(SeatType.NORMAL, SeatType.COUPLE);
    }

    @Test
    void requiresVerifiedCustomerBeforeAccessingInventory() {
        when(currentUserService.getVerifiedCustomer())
                .thenThrow(new ForbiddenException("Email verification is required"));

        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(ForbiddenException.class);
        verify(showtimeRepository, never()).findById(any());
    }

    @Test
    void rejectsCancelledOrPastShowtime() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        showtime.setStatus(ShowtimeStatus.CANCELLED);

        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active showtime");

        showtime.setStatus(ShowtimeStatus.ACTIVE);
        showtime.setStartTime(NOW);
        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past or started");
    }

    @Test
    void rejectsBookedHeldUnavailableOrInactiveSeat() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSelectedSeatsForUpdate(4L, List.of(10L)))
                .thenReturn(List.of(normalInventory));

        normalInventory.setStatus(SeatInventoryStatus.BOOKED);
        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(ConflictException.class);

        normalInventory.setStatus(SeatInventoryStatus.UNAVAILABLE);
        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(ConflictException.class);

        normalInventory.setStatus(SeatInventoryStatus.AVAILABLE);
        normalInventory.getSeat().setActive(false);
        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void releasesExpiredHoldBeforeAvailabilityRecheck() {
        Booking oldBooking = pendingBooking(70L, "QS-OLD", NOW.minusSeconds(10));
        normalInventory.setStatus(SeatInventoryStatus.HELD);
        normalInventory.setHeldByBooking(oldBooking);
        normalInventory.setHoldExpiresAt(NOW.minusSeconds(10));
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSelectedSeatsForUpdate(4L, List.of(10L)))
                .thenReturn(List.of(normalInventory));
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(70L))
                .thenReturn(List.of(normalInventory));
        mockBookingSave();

        var response = service.hold(new SeatHoldRequest(4L, List.of(10L)));

        assertThat(oldBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(normalInventory.getHeldByBooking().getId()).isEqualTo(response.bookingId());
    }

    @Test
    void manuallyReleasesPendingHold() {
        Booking booking = pendingBooking(80L, "QS-MANUAL", NOW.plusSeconds(300));
        normalInventory.setStatus(SeatInventoryStatus.HELD);
        normalInventory.setHeldByBooking(booking);
        normalInventory.setHoldExpiresAt(booking.getExpiresAt());
        BookingSeat snapshot = snapshot(booking, normalInventory.getSeat(), new BigDecimal("5000.00"));
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(bookingRepository.findCustomerBookingForUpdate("QS-MANUAL", 50L))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findByBookingIdWithSeats(80L)).thenReturn(List.of(snapshot));
        when(showtimeSeatRepository.findHeldSeatsByBookingIdForUpdate(80L))
                .thenReturn(List.of(normalInventory));

        var response = service.release("QS-MANUAL");

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.remainingSeconds()).isZero();
        assertThat(normalInventory.getStatus()).isEqualTo(SeatInventoryStatus.AVAILABLE);
        assertThat(normalInventory.getHeldByBooking()).isNull();
        assertThat(normalInventory.getHoldExpiresAt()).isNull();
    }

    @Test
    void scheduledCleanupExpiresBookingsAndReleasesSeats() {
        Booking booking = pendingBooking(90L, "QS-EXPIRED", NOW.minusSeconds(1));
        normalInventory.setStatus(SeatInventoryStatus.HELD);
        normalInventory.setHeldByBooking(booking);
        normalInventory.setHoldExpiresAt(NOW.minusSeconds(1));
        when(showtimeSeatRepository.findExpiredHoldsForUpdate(SeatInventoryStatus.HELD, NOW))
                .thenReturn(List.of(normalInventory));

        service.releaseExpiredHolds();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(normalInventory.getStatus()).isEqualTo(SeatInventoryStatus.AVAILABLE);
        assertThat(normalInventory.getHeldByBooking()).isNull();
    }

    @Test
    void snapshotFailureDoesNotLeaveSeatMarkedAsHeld() {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSelectedSeatsForUpdate(4L, List.of(10L)))
                .thenReturn(List.of(normalInventory));
        mockBookingSave();
        when(bookingSeatRepository.saveAll(anyList())).thenThrow(new RuntimeException("write failed"));

        assertThatThrownBy(() -> service.hold(new SeatHoldRequest(4L, List.of(10L))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("write failed");
        assertThat(normalInventory.getStatus()).isEqualTo(SeatInventoryStatus.AVAILABLE);
        assertThat(normalInventory.getHeldByBooking()).isNull();
    }

    private void mockValidHold(List<ShowtimeSeat> selectedSeats) {
        when(currentUserService.getVerifiedCustomer()).thenReturn(customer);
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSelectedSeatsForUpdate(4L, List.of(10L, 11L)))
                .thenReturn(selectedSeats);
        mockBookingSave();
        when(bookingSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void mockBookingSave() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(100L);
            return booking;
        });
    }

    private ShowtimeSeat inventory(Long id, String row, int number, SeatType type, boolean active) {
        Seat seat = new Seat();
        seat.setId(id + 100);
        seat.setScreen(showtime.getScreen());
        seat.setRowName(row);
        seat.setSeatNumber(number);
        seat.setSeatType(type);
        seat.setActive(active);
        ShowtimeSeat inventory = new ShowtimeSeat();
        inventory.setId(id);
        inventory.setShowtime(showtime);
        inventory.setSeat(seat);
        inventory.setStatus(active ? SeatInventoryStatus.AVAILABLE : SeatInventoryStatus.UNAVAILABLE);
        return inventory;
    }

    private Booking pendingBooking(Long id, String reference, Instant expiresAt) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setBookingReference(reference);
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("5000.00"));
        booking.setExpiresAt(expiresAt);
        return booking;
    }

    private BookingSeat snapshot(Booking booking, Seat seat, BigDecimal price) {
        BookingSeat snapshot = new BookingSeat();
        snapshot.setBooking(booking);
        snapshot.setSeat(seat);
        snapshot.setSeatType(seat.getSeatType());
        snapshot.setUnitPrice(price);
        return snapshot;
    }
}
