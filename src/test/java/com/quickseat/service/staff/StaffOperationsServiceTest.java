package com.quickseat.service.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.quickseat.entity.User;
import com.quickseat.entity.enums.BookingStatus;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.repository.BookingRepository;
import com.quickseat.repository.BookingSeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.security.CurrentUserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StaffOperationsServiceTest {
    @Mock ShowtimeRepository showtimeRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock CurrentUserService currentUserService;

    private StaffOperationsService service;
    private User staff;
    private Cinema cinema;
    private Showtime showtime;
    private Booking booking;
    private BookingSeat bookingSeat;

    @BeforeEach
    void setUp() {
        service = new StaffOperationsService(showtimeRepository, showtimeSeatRepository,
                bookingRepository, bookingSeatRepository, currentUserService);
        cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("Cinema One");
        cinema.setAddress("Main Road");
        cinema.setCity("Yangon");
        cinema.setActive(true);
        staff = new User();
        staff.setId(2L);
        staff.setCinema(cinema);

        Screen screen = new Screen();
        screen.setId(3L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setId(4L);
        movie.setTitle("Staff Movie");
        showtime = new Showtime();
        showtime.setId(5L);
        showtime.setScreen(screen);
        showtime.setMovie(movie);
        showtime.setStartTime(Instant.parse("2026-09-11T10:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-09-11T12:15:00Z"));
        showtime.setCleaningBufferMinutes(15);
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        showtime.setStatus(ShowtimeStatus.ACTIVE);

        User customer = new User();
        customer.setId(6L);
        customer.setName("Customer");
        customer.setEmail("customer@example.com");
        booking = new Booking();
        booking.setId(7L);
        booking.setBookingReference("QS-STAFF");
        booking.setUser(customer);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(new BigDecimal("5000.00"));

        Seat seat = new Seat();
        seat.setId(8L);
        seat.setRowName("A");
        seat.setSeatNumber(1);
        seat.setSeatType(SeatType.NORMAL);
        bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setSeat(seat);
        bookingSeat.setSeatType(SeatType.NORMAL);
        bookingSeat.setUnitPrice(new BigDecimal("5000.00"));
    }

    @Test
    void returnsAssignedCinema() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);

        var result = service.getAssignedCinema();

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Cinema One");
    }

    @Test
    void listsOnlyAssignedCinemaShowtimesWithFiltersAndPagination() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(showtimeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(showtime)));

        var result = service.listShowtimes(4L, LocalDate.of(2026, 9, 11),
                ShowtimeStatus.ACTIVE, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().cinemaId()).isEqualTo(1L);
    }

    @Test
    void crossCinemaShowtimeDetailIsRejected() {
        Cinema otherCinema = new Cinema();
        otherCinema.setId(99L);
        showtime.getScreen().setCinema(otherCinema);
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(showtimeRepository.findById(5L)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> service.getShowtime(5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another cinema");
    }

    @Test
    void returnsAssignedCinemaShowtimeSeatStatusAndPrice() {
        ShowtimeSeat inventory = new ShowtimeSeat();
        inventory.setId(9L);
        inventory.setShowtime(showtime);
        inventory.setSeat(bookingSeat.getSeat());
        inventory.setStatus(SeatInventoryStatus.BOOKED);
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(showtimeRepository.findById(5L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSeatMapByShowtimeId(5L)).thenReturn(List.of(inventory));

        var result = service.getShowtimeSeats(5L);

        assertThat(result.seats()).hasSize(1);
        assertThat(result.seats().getFirst().status()).isEqualTo(SeatInventoryStatus.BOOKED);
        assertThat(result.seats().getFirst().price()).isEqualByComparingTo("5000.00");
    }

    @Test
    void listsCinemaBookingsWithSearchAndFilters() {
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingSeatRepository.findByBookingIdsWithSeats(List.of(7L)))
                .thenReturn(List.of(bookingSeat));

        var result = service.listBookings(BookingStatus.CONFIRMED, 5L,
                LocalDate.of(2026, 9, 11), "QS-STAFF", 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().customerEmail()).isEqualTo("customer@example.com");
        assertThat(result.content().getFirst().seats()).hasSize(1);
        verify(bookingRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void crossCinemaBookingDetailIsRejected() {
        Cinema otherCinema = new Cinema();
        otherCinema.setId(99L);
        booking.getShowtime().getScreen().setCinema(otherCinema);
        when(currentUserService.getActiveStaffWithCinema()).thenReturn(staff);
        when(bookingRepository.findByBookingReference("QS-STAFF")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getBooking("QS-STAFF"))
                .isInstanceOf(ForbiddenException.class);
    }
}
