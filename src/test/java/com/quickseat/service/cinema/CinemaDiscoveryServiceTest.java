package com.quickseat.service.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.entity.Cinema;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CinemaDiscoveryServiceTest {
    @Mock CinemaRepository cinemaRepository;

    @Test
    void listsActiveCinemasWithSearchCityAndPagination() {
        Cinema cinema = cinema(1L, true);
        when(cinemaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cinema)));
        CinemaDiscoveryService service = new CinemaDiscoveryService(cinemaRepository);

        var result = service.list("  Downtown ", " Yangon ", 1, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(cinemaRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().name()).isEqualTo("Downtown Cinema");
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void returnsOnlyActiveCinemaDetails() {
        Cinema activeCinema = cinema(1L, true);
        when(cinemaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(activeCinema));
        when(cinemaRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.empty());
        CinemaDiscoveryService service = new CinemaDiscoveryService(cinemaRepository);

        assertThat(service.get(1L).active()).isTrue();
        assertThatThrownBy(() -> service.get(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cinema not found");
    }

    private Cinema cinema(Long id, boolean active) {
        Cinema cinema = new Cinema();
        cinema.setId(id);
        cinema.setName("Downtown Cinema");
        cinema.setAddress("Main Road");
        cinema.setCity("Yangon");
        cinema.setActive(active);
        return cinema;
    }
}
