package com.quickseat.service.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ShowtimeDiscoveryServiceTest {
    @Mock ShowtimeRepository showtimeRepository;

    @Test
    void returnsDiscoveryFieldsOrderedByStartTime() {
        Instant now = Instant.parse("2026-09-12T00:00:00Z");
        Showtime showtime = showtime(now.plusSeconds(3600));
        when(showtimeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(showtime)));
        ShowtimeDiscoveryService service = new ShowtimeDiscoveryService(
                showtimeRepository, Clock.fixed(now, ZoneOffset.UTC));

        var result = service.list(30L, 10L, LocalDate.of(2026, 9, 12), 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(showtimeRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().showtimeId()).isEqualTo(1L);
        assertThat(result.content().getFirst().movieTitle()).isEqualTo("Test Movie");
        assertThat(result.content().getFirst().cinemaName()).isEqualTo("Downtown Cinema");
        assertThat(result.content().getFirst().normalPrice()).isEqualByComparingTo("5000.00");
        assertThat(pageable.getValue().getSort().getOrderFor("startTime")).isNotNull();
    }

    private Showtime showtime(Instant startTime) {
        Cinema cinema = new Cinema();
        cinema.setId(10L);
        cinema.setName("Downtown Cinema");
        cinema.setActive(true);

        Screen screen = new Screen();
        screen.setId(20L);
        screen.setName("Screen 1");
        screen.setCinema(cinema);
        screen.setActive(true);

        Movie movie = new Movie();
        movie.setId(30L);
        movie.setTitle("Test Movie");
        movie.setActive(true);

        Showtime showtime = new Showtime();
        showtime.setId(1L);
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(startTime);
        showtime.setEndTime(startTime.plusSeconds(7200));
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        return showtime;
    }
}
