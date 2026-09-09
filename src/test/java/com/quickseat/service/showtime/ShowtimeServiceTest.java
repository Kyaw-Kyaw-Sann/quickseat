package com.quickseat.service.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.showtime.ShowtimeRequest;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.repository.MovieRepository;
import com.quickseat.repository.ScreenRepository;
import com.quickseat.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {
    @Mock ShowtimeRepository showtimeRepository;
    @Mock MovieRepository movieRepository;
    @Mock ScreenRepository screenRepository;

    private ShowtimeService service;
    private Movie movie;
    private Screen screen;

    @BeforeEach
    void setUp() {
        service = new ShowtimeService(showtimeRepository, movieRepository, screenRepository);
        Cinema cinema = new Cinema();
        cinema.setId(10L);
        cinema.setName("Downtown Cinema");
        cinema.setActive(true);
        screen = new Screen();
        screen.setId(20L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        screen.setActive(true);
        movie = new Movie();
        movie.setId(30L);
        movie.setTitle("Test Movie");
        movie.setDurationMinutes(120);
        movie.setActive(true);
    }

    @Test
    void createsShowtimeAndCalculatesEndTimeWithCleaningBuffer() {
        Instant start = Instant.now().plusSeconds(86_400);
        mockActiveDependencies();
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(invocation -> {
            Showtime showtime = invocation.getArgument(0);
            showtime.setId(1L);
            return showtime;
        });

        var response = service.create(request(start, 15));

        assertThat(response.endTime()).isEqualTo(start.plusSeconds(135 * 60L));
        assertThat(response.status()).isEqualTo(ShowtimeStatus.ACTIVE);
        verify(showtimeRepository).existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                20L, ShowtimeStatus.ACTIVE, start.plusSeconds(135 * 60L), start);
    }

    @Test
    void rejectsOverlappingActiveShowtime() {
        Instant start = Instant.now().plusSeconds(86_400);
        mockActiveDependencies();
        when(showtimeRepository.existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(20L), eq(ShowtimeStatus.ACTIVE), any(Instant.class), eq(start))).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(start, 15)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void cancelledShowtimesAreExcludedFromOverlapCheckAndDoNotBlockCreation() {
        Instant start = Instant.now().plusSeconds(86_400);
        mockActiveDependencies();
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request(start, 10));

        verify(showtimeRepository).existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(20L), eq(ShowtimeStatus.ACTIVE), any(Instant.class), eq(start));
    }

    @Test
    void updateExcludesCurrentShowtimeFromOverlapCheck() {
        Instant start = Instant.now().plusSeconds(172_800);
        Showtime existing = showtime(7L, ShowtimeStatus.ACTIVE, start.minusSeconds(3600));
        when(showtimeRepository.findById(7L)).thenReturn(Optional.of(existing));
        mockActiveDependencies();

        service.update(7L, request(start, 15));

        verify(showtimeRepository).existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                eq(20L), eq(ShowtimeStatus.ACTIVE), any(Instant.class), eq(start), eq(7L));
        assertThat(existing.getEndTime()).isEqualTo(start.plusSeconds(135 * 60L));
    }

    @Test
    void rejectsInactiveMovieScreenOrCinema() {
        Instant start = Instant.now().plusSeconds(86_400);
        movie.setActive(false);
        when(movieRepository.findById(30L)).thenReturn(Optional.of(movie));
        assertThatThrownBy(() -> service.create(request(start, 15)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive movie");

        movie.setActive(true);
        screen.setActive(false);
        when(screenRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(screen));
        assertThatThrownBy(() -> service.create(request(start, 15)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive screen");
    }

    @Test
    void filtersByMovieCinemaDateAndStatusWithPagination() {
        Showtime showtime = showtime(1L, ShowtimeStatus.ACTIVE, Instant.now().plusSeconds(86_400));
        when(showtimeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(showtime)));

        var result = service.list(30L, 10L, LocalDate.of(2026, 9, 10), ShowtimeStatus.ACTIVE, 1, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(showtimeRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(result.content()).hasSize(1);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void cancelsShowtime() {
        Showtime showtime = showtime(1L, ShowtimeStatus.ACTIVE, Instant.now().plusSeconds(86_400));
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));

        assertThat(service.cancel(1L).status()).isEqualTo(ShowtimeStatus.CANCELLED);
    }

    private void mockActiveDependencies() {
        when(movieRepository.findById(30L)).thenReturn(Optional.of(movie));
        when(screenRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(screen));
    }

    private ShowtimeRequest request(Instant start, int buffer) {
        return new ShowtimeRequest(30L, 20L, start, new BigDecimal("5000.00"),
                new BigDecimal("9000.00"), buffer);
    }

    private Showtime showtime(Long id, ShowtimeStatus status, Instant start) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(start);
        showtime.setEndTime(start.plusSeconds(135 * 60L));
        showtime.setCleaningBufferMinutes(15);
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        showtime.setStatus(status);
        return showtime;
    }
}
