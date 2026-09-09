package com.quickseat.service.movie;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.movie.MovieRequest;
import com.quickseat.dto.response.movie.MovieResponse;
import com.quickseat.entity.Movie;
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.MovieRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        Movie saved = movieRepository.save(movie);
        log.info("Movie created with id {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = getEntity(id);
        apply(movie, request);
        log.info("Movie {} updated", id);
        return toResponse(movie);
    }

    @Transactional
    public MovieResponse setActive(Long id, boolean active) {
        Movie movie = getEntity(id);
        movie.setActive(active);
        log.info("Movie {} active status changed to {}", id, active);
        return toResponse(movie);
    }

    @Transactional
    public MovieResponse setStatus(Long id, MovieStatus status) {
        Movie movie = getEntity(id);
        movie.setStatus(status);
        log.info("Movie {} lifecycle status changed to {}", id, status);
        return toResponse(movie);
    }

    public MovieResponse getPublic(Long id) {
        return movieRepository.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    public MovieResponse getAdmin(Long id) {
        return toResponse(getEntity(id));
    }

    public PageResponse<MovieResponse> listPublic(String search, MovieStatus status, String language, int page, int size) {
        return list(search, status, language, true, page, size);
    }

    public PageResponse<MovieResponse> listAdmin(String search, MovieStatus status, String language, Boolean active,
                                                 int page, int size) {
        return list(search, status, language, active, page, size);
    }

    private PageResponse<MovieResponse> list(String search, MovieStatus status, String language, Boolean active,
                                             int page, int size) {
        String normalizedSearch = trimToNull(search);
        String normalizedLanguage = trimToNull(language);
        Specification<Movie> specification = (root, query, builder) -> builder.conjunction();

        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("director")), pattern),
                    builder.like(builder.lower(root.get("castText")), pattern)));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (normalizedLanguage != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("language")), normalizedLanguage.toLowerCase()));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }

        Sort sort = Sort.by(Sort.Order.desc("releaseDate").nullsLast(), Sort.Order.desc("createdAt"));
        Page<MovieResponse> result = movieRepository.findAll(specification, PageRequest.of(page, size, sort))
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    Movie getEntity(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
    }

    private void apply(Movie movie, MovieRequest request) {
        movie.setTitle(request.title().trim());
        movie.setDescription(trimToNull(request.description()));
        movie.setDurationMinutes(request.durationMinutes());
        movie.setReleaseDate(request.releaseDate());
        movie.setLanguage(trimToNull(request.language()));
        movie.setGenres(normalizeGenres(request.genres()));
        movie.setAgeRating(trimToNull(request.ageRating()));
        movie.setDirector(trimToNull(request.director()));
        movie.setCastText(trimToNull(request.castText()));
        movie.setPosterUrl(trimToNull(request.posterUrl()));
        movie.setTrailerUrl(trimToNull(request.trailerUrl()));
        movie.setStatus(request.status());
    }

    private String[] normalizeGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return new String[0];
        }
        return genres.stream().map(String::trim).distinct().toArray(String[]::new);
    }

    private MovieResponse toResponse(Movie movie) {
        List<String> genres = movie.getGenres() == null ? List.of() : Arrays.asList(movie.getGenres());
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(), movie.getDurationMinutes(),
                movie.getReleaseDate(), movie.getLanguage(), genres, movie.getAgeRating(), movie.getDirector(),
                movie.getCastText(), movie.getPosterUrl(), movie.getTrailerUrl(), movie.getStatus(), movie.isActive(),
                movie.getCreatedAt(), movie.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
