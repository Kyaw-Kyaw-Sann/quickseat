package com.quickseat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.quickseat.dto.ScreenRequest;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Screen;
import com.quickseat.exception.ConflictException;
import com.quickseat.repository.CinemaRepository;
import com.quickseat.repository.ScreenRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {
    @Mock ScreenRepository screenRepository;
    @Mock CinemaRepository cinemaRepository;

    @Test void rejectsDuplicateNameWithinCinema() {
        Cinema cinema = cinema();
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(screenRepository.existsByCinemaIdAndNameIgnoreCase(1L, "Screen 1")).thenReturn(true);
        ScreenService service = new ScreenService(screenRepository, cinemaRepository);
        assertThatThrownBy(() -> service.create(1L, new ScreenRequest("Screen 1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test void createsAndDeactivatesScreen() {
        Cinema cinema = cinema();
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(screenRepository.save(any(Screen.class))).thenAnswer(invocation -> {
            Screen screen = invocation.getArgument(0); screen.setId(2L); return screen;
        });
        ScreenService service = new ScreenService(screenRepository, cinemaRepository);
        assertThat(service.create(1L, new ScreenRequest("Screen 1")).cinemaId()).isEqualTo(1L);
        Screen screen = new Screen(); screen.setId(2L); screen.setCinema(cinema); screen.setActive(true);
        when(screenRepository.findById(2L)).thenReturn(Optional.of(screen));
        assertThat(service.setActive(2L, false).active()).isFalse();
    }

    private Cinema cinema() { Cinema cinema = new Cinema(); cinema.setId(1L); cinema.setActive(true); return cinema; }
}
