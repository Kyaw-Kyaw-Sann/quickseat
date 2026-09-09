package com.quickseat.service.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.movie.MovieRequest;
import com.quickseat.entity.Movie;
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.MovieRepository;
import java.time.LocalDate;
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
class MovieServiceTest {
    @Mock MovieRepository movieRepository;

    @Test
    void createsMovieAndNormalizesValues() {
        MovieService service = new MovieService(movieRepository);
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie movie = invocation.getArgument(0);
            movie.setId(1L);
            return movie;
        });

        var response = service.create(request(MovieStatus.UPCOMING));

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(captor.capture());
        assertThat(response.id()).isEqualTo(1L);
        assertThat(captor.getValue().getTitle()).isEqualTo("Inception");
        assertThat(captor.getValue().getGenres()).containsExactly("Sci-Fi", "Drama");
        assertThat(captor.getValue().getStatus()).isEqualTo(MovieStatus.UPCOMING);
    }

    @Test
    void updatesStatusAndActiveState() {
        Movie movie = movie(true, MovieStatus.UPCOMING);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        MovieService service = new MovieService(movieRepository);

        assertThat(service.setStatus(1L, MovieStatus.NOW_SHOWING).status()).isEqualTo(MovieStatus.NOW_SHOWING);
        assertThat(service.setActive(1L, false).active()).isFalse();
    }

    @Test
    void publicQueriesOnlyReturnActiveMoviesAndSupportPagination() {
        Movie movie = movie(true, MovieStatus.NOW_SHOWING);
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movie)));
        MovieService service = new MovieService(movieRepository);

        var page = service.listPublic("incep", MovieStatus.NOW_SHOWING, "English", 1, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(movieRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(page.content()).hasSize(1);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void publicDetailRejectsInactiveOrMissingMovie() {
        when(movieRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());
        MovieService service = new MovieService(movieRepository);

        assertThatThrownBy(() -> service.getPublic(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private MovieRequest request(MovieStatus status) {
        return new MovieRequest(" Inception ", " Dream thriller ", 148, LocalDate.of(2010, 7, 16),
                " English ", List.of("Sci-Fi", "Drama", "Sci-Fi"), "PG-13", "Christopher Nolan",
                "Leonardo DiCaprio", "https://example.com/poster.jpg", "https://example.com/trailer", status);
    }

    private Movie movie(boolean active, MovieStatus status) {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");
        movie.setDurationMinutes(148);
        movie.setGenres(new String[]{"Sci-Fi"});
        movie.setStatus(status);
        movie.setActive(active);
        return movie;
    }
}
