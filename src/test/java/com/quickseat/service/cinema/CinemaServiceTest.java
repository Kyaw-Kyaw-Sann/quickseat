package com.quickseat.service.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.cinema.CinemaRequest;
import com.quickseat.entity.Cinema;
import com.quickseat.repository.CinemaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {
    @Mock CinemaRepository cinemaRepository;

    @Test void createsAndDeactivatesCinema() {
        CinemaService service = new CinemaService(cinemaRepository);
        when(cinemaRepository.save(any(Cinema.class))).thenAnswer(invocation -> {
            Cinema cinema = invocation.getArgument(0); cinema.setId(1L); return cinema;
        });
        var created = service.create(new CinemaRequest("Downtown", "Main Road", "Yangon", null, null));
        assertThat(created.name()).isEqualTo("Downtown");
        Cinema cinema = new Cinema(); cinema.setId(1L); cinema.setActive(true);
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        assertThat(service.setActive(1L, false).active()).isFalse();
    }
}
