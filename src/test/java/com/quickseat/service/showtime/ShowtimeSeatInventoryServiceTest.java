package com.quickseat.service.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.SeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowtimeSeatInventoryServiceTest {
    @Mock ShowtimeRepository showtimeRepository;
    @Mock SeatRepository seatRepository;
    @Mock ShowtimeSeatRepository showtimeSeatRepository;

    private ShowtimeSeatInventoryService service;
    private Showtime showtime;

    @BeforeEach
    void setUp() {
        service = new ShowtimeSeatInventoryService(showtimeRepository, seatRepository, showtimeSeatRepository);

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("QuickSeat Cinema");
        cinema.setActive(true);

        Screen screen = new Screen();
        screen.setId(2L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        screen.setActive(true);

        Movie movie = new Movie();
        movie.setId(3L);
        movie.setTitle("Inventory Movie");
        movie.setActive(true);

        showtime = new Showtime();
        showtime.setId(4L);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(Instant.parse("2026-09-20T10:00:00Z"));
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        showtime.setStatus(ShowtimeStatus.ACTIVE);
    }

    @Test
    void generatesInventoryFromScreenLayoutWithCorrectStatusesPricesAndOrder() {
        Seat normalSeat = seat(10L, "A", 1, SeatType.NORMAL, true);
        Seat coupleSeat = seat(11L, "A", 2, SeatType.COUPLE, true);
        Seat inactiveSeat = seat(12L, "B", 1, SeatType.NORMAL, false);
        when(showtimeRepository.findByIdForInventoryGeneration(4L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByScreenIdOrderByRowNameAscSeatNumberAsc(2L))
                .thenReturn(List.of(normalSeat, coupleSeat, inactiveSeat));
        when(showtimeSeatRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ShowtimeSeat> inventory = invocation.getArgument(0);
            AtomicLong id = new AtomicLong(100L);
            inventory.forEach(item -> item.setId(id.getAndIncrement()));
            return inventory;
        });

        var result = service.generate(4L);

        assertThat(result.showtimeId()).isEqualTo(4L);
        assertThat(result.movieTitle()).isEqualTo("Inventory Movie");
        assertThat(result.screenName()).isEqualTo("Screen 1");
        assertThat(result.seats()).extracting(seat -> seat.rowName() + seat.seatNumber())
                .containsExactly("A1", "A2", "B1");
        assertThat(result.seats().get(0).price()).isEqualByComparingTo("5000.00");
        assertThat(result.seats().get(1).price()).isEqualByComparingTo("9000.00");
        assertThat(result.seats()).extracting(seat -> seat.status())
                .containsExactly(SeatInventoryStatus.AVAILABLE, SeatInventoryStatus.AVAILABLE,
                        SeatInventoryStatus.UNAVAILABLE);
    }

    @Test
    void rejectsDuplicateInventoryGeneration() {
        when(showtimeRepository.findByIdForInventoryGeneration(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.existsByShowtimeId(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.generate(4L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been generated");
        verify(seatRepository, never()).findByScreenIdOrderByRowNameAscSeatNumberAsc(2L);
    }

    @Test
    void rejectsGenerationForCancelledShowtime() {
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        when(showtimeRepository.findByIdForInventoryGeneration(4L)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> service.generate(4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active showtime");
    }

    @Test
    void rejectsGenerationWhenScreenHasNoSeats() {
        when(showtimeRepository.findByIdForInventoryGeneration(4L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByScreenIdOrderByRowNameAscSeatNumberAsc(2L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generate(4L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no seats");
    }

    @Test
    void returnsSeatMapInRepositoryOrderWithCorrectPrices() {
        ShowtimeSeat first = inventorySeat(101L, seat(10L, "A", 1, SeatType.NORMAL, true));
        ShowtimeSeat second = inventorySeat(102L, seat(11L, "A", 2, SeatType.COUPLE, true));
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSeatMapByShowtimeId(4L)).thenReturn(List.of(first, second));

        var result = service.getSeatMap(4L);

        assertThat(result.seats()).extracting(seat -> seat.seatNumber()).containsExactly(1, 2);
        assertThat(result.seats()).extracting(seat -> seat.price())
                .containsExactly(new BigDecimal("5000.00"), new BigDecimal("9000.00"));
        verify(showtimeSeatRepository).findSeatMapByShowtimeId(4L);
    }

    @Test
    void rejectsSeatMapWhenInventoryWasNotGenerated() {
        when(showtimeRepository.findById(4L)).thenReturn(Optional.of(showtime));
        when(showtimeSeatRepository.findSeatMapByShowtimeId(4L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getSeatMap(4L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not been generated");
    }

    private Seat seat(Long id, String rowName, int seatNumber, SeatType type, boolean active) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setScreen(showtime.getScreen());
        seat.setRowName(rowName);
        seat.setSeatNumber(seatNumber);
        seat.setSeatType(type);
        seat.setActive(active);
        return seat;
    }

    private ShowtimeSeat inventorySeat(Long id, Seat seat) {
        ShowtimeSeat inventorySeat = new ShowtimeSeat();
        inventorySeat.setId(id);
        inventorySeat.setShowtime(showtime);
        inventorySeat.setSeat(seat);
        inventorySeat.setStatus(SeatInventoryStatus.AVAILABLE);
        return inventorySeat;
    }
}
