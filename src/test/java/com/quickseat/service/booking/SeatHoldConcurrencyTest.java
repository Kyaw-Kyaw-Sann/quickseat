package com.quickseat.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.booking.SeatHoldRequest;
import com.quickseat.entity.Booking;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.ConflictException;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SeatHoldConcurrencyTest {
    @Test
    void exactlyOneOfTwoCustomersCanHoldTheSameSeat() throws Exception {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingSeatRepository bookingSeatRepository = mock(BookingSeatRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        ShowtimeSeatRepository showtimeSeatRepository = mock(ShowtimeSeatRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Instant now = Instant.parse("2026-09-10T10:00:00Z");
        SeatHoldService service = new SeatHoldService(bookingRepository, bookingSeatRepository,
                showtimeRepository, showtimeSeatRepository, currentUserService,
                Clock.fixed(now, ZoneOffset.UTC), 5);

        Showtime showtime = showtime(now);
        ShowtimeSeat sharedSeat = inventory(showtime);
        ThreadLocal<User> currentCustomer = new ThreadLocal<>();
        when(currentUserService.getVerifiedCustomer()).thenAnswer(invocation -> currentCustomer.get());
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch firstFinished = new CountDownLatch(1);
        AtomicInteger lockCalls = new AtomicInteger();
        when(showtimeSeatRepository.findSelectedSeatsForUpdate(4L, List.of(10L))).thenAnswer(invocation -> {
            if (lockCalls.incrementAndGet() == 1) {
                firstLocked.countDown();
                secondStarted.await(5, TimeUnit.SECONDS);
            } else {
                firstFinished.await(5, TimeUnit.SECONDS);
            }
            return List.of(sharedSeat);
        });
        AtomicInteger bookingIds = new AtomicInteger(100);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId((long) bookingIds.getAndIncrement());
            return booking;
        });
        when(bookingSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        User customerA = customer(1L);
        User customerB = customer(2L);
        SeatHoldRequest request = new SeatHoldRequest(4L, List.of(10L));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var winner = executor.submit(() -> {
                currentCustomer.set(customerA);
                try {
                    return service.hold(request);
                } finally {
                    firstFinished.countDown();
                    currentCustomer.remove();
                }
            });
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            var loser = executor.submit(() -> {
                currentCustomer.set(customerB);
                secondStarted.countDown();
                try {
                    service.hold(request);
                    return null;
                } catch (RuntimeException exception) {
                    return exception;
                } finally {
                    currentCustomer.remove();
                }
            });

            assertThat(winner.get(5, TimeUnit.SECONDS).bookingId()).isNotNull();
            assertThat(loser.get(5, TimeUnit.SECONDS)).isInstanceOf(ConflictException.class);
        }
        assertThat(sharedSeat.getStatus()).isEqualTo(SeatInventoryStatus.HELD);
        assertThat(sharedSeat.getHeldByBooking().getUser().getId()).isEqualTo(1L);
    }

    private Showtime showtime(Instant now) {
        Cinema cinema = new Cinema();
        cinema.setActive(true);
        Screen screen = new Screen();
        screen.setId(2L);
        screen.setActive(true);
        screen.setCinema(cinema);
        Movie movie = new Movie();
        movie.setActive(true);
        Showtime showtime = new Showtime();
        showtime.setId(4L);
        showtime.setScreen(screen);
        showtime.setMovie(movie);
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        showtime.setStartTime(now.plusSeconds(3_600));
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        return showtime;
    }

    private ShowtimeSeat inventory(Showtime showtime) {
        Seat seat = new Seat();
        seat.setId(20L);
        seat.setScreen(showtime.getScreen());
        seat.setRowName("A");
        seat.setSeatNumber(5);
        seat.setSeatType(SeatType.NORMAL);
        seat.setActive(true);
        ShowtimeSeat inventory = new ShowtimeSeat();
        inventory.setId(10L);
        inventory.setShowtime(showtime);
        inventory.setSeat(seat);
        inventory.setStatus(SeatInventoryStatus.AVAILABLE);
        return inventory;
    }

    private User customer(Long id) {
        User customer = new User();
        customer.setId(id);
        customer.setActive(true);
        customer.setEmailVerified(true);
        return customer;
    }
}
