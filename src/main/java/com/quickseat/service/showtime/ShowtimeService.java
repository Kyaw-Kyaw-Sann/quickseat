package com.quickseat.service.showtime;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.showtime.ShowtimeRequest;
import com.quickseat.dto.response.showtime.ShowtimeResponse;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.MovieRepository;
import com.quickseat.repository.ScreenRepository;
import com.quickseat.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
public class ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;

    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        validateRequest(request);
        Movie movie = getActiveMovie(request.movieId());
        Screen screen = getActiveScreenForUpdate(request.screenId());
        Instant endTime = calculateEndTime(request.startTime(), movie.getDurationMinutes(),
                request.cleaningBufferMinutes());
        ensureNoOverlap(screen.getId(), request.startTime(), endTime, null);

        Showtime showtime = new Showtime();
        apply(showtime, request, movie, screen, endTime);
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        Showtime saved = showtimeRepository.save(showtime);
        log.info("Showtime created with id {} for screen {}", saved.getId(), screen.getId());
        return toResponse(saved);
    }

    @Transactional
    public ShowtimeResponse update(Long id, ShowtimeRequest request) {
        validateRequest(request);
        Showtime showtime = getEntity(id);
        if (showtime.getStatus() != ShowtimeStatus.ACTIVE) {
            throw new BadRequestException("Only active showtimes can be updated");
        }

        Movie movie = getActiveMovie(request.movieId());
        Screen screen = getActiveScreenForUpdate(request.screenId());
        Instant endTime = calculateEndTime(request.startTime(), movie.getDurationMinutes(),
                request.cleaningBufferMinutes());
        ensureNoOverlap(screen.getId(), request.startTime(), endTime, id);
        apply(showtime, request, movie, screen, endTime);
        log.info("Showtime {} updated", id);
        return toResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse cancel(Long id) {
        Showtime showtime = getEntity(id);
        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new BadRequestException("Completed showtime cannot be cancelled");
        }
        if (showtime.getStatus() != ShowtimeStatus.CANCELLED) {
            showtime.setStatus(ShowtimeStatus.CANCELLED);
            log.info("Showtime {} cancelled", id);
        }
        return toResponse(showtime);
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> list(Long movieId, Long cinemaId, LocalDate date,
                                               ShowtimeStatus status, int page, int size) {
        Specification<Showtime> specification = (root, query, builder) -> builder.conjunction();
        if (movieId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movie").get("id"), movieId));
        }
        if (cinemaId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("screen").get("cinema").get("id"), cinemaId));
        }
        if (date != null) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant nextDayStart = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.greaterThanOrEqualTo(root.get("startTime"), dayStart),
                    builder.lessThan(root.get("startTime"), nextDayStart)));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }

        Page<ShowtimeResponse> result = showtimeRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime")))
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    private Movie getActiveMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        if (!movie.isActive()) {
            throw new BadRequestException("Cannot schedule an inactive movie");
        }
        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
            throw new BadRequestException("Movie duration must be valid");
        }
        return movie;
    }

    private Screen getActiveScreenForUpdate(Long id) {
        Screen screen = screenRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        if (!screen.isActive()) {
            throw new BadRequestException("Cannot schedule on an inactive screen");
        }
        if (!screen.getCinema().isActive()) {
            throw new BadRequestException("Cannot schedule in an inactive cinema");
        }
        return screen;
    }

    private void validateRequest(ShowtimeRequest request) {
        if (request.startTime() == null || !request.startTime().isAfter(Instant.now())) {
            throw new BadRequestException("Showtime start time must be in the future");
        }
        validatePrice(request.normalPrice(), "Normal seat price");
        validatePrice(request.couplePrice(), "Couple seat price");
        if (request.cleaningBufferMinutes() == null
                || request.cleaningBufferMinutes() < 0
                || request.cleaningBufferMinutes() > 180) {
            throw new BadRequestException("Cleaning buffer must be between 0 and 180 minutes");
        }
    }

    private void validatePrice(BigDecimal price, String fieldName) {
        if (price == null || price.signum() <= 0) {
            throw new BadRequestException(fieldName + " must be greater than zero");
        }
    }

    private Instant calculateEndTime(Instant startTime, int movieDurationMinutes, int cleaningBufferMinutes) {
        return startTime.plusSeconds((long) (movieDurationMinutes + cleaningBufferMinutes) * 60);
    }

    private void ensureNoOverlap(Long screenId, Instant startTime, Instant endTime, Long excludedId) {
        boolean overlaps = excludedId == null
                ? showtimeRepository.existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        screenId, ShowtimeStatus.ACTIVE, endTime, startTime)
                : showtimeRepository.existsByScreenIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        screenId, ShowtimeStatus.ACTIVE, endTime, startTime, excludedId);
        if (overlaps) {
            throw new ConflictException("Showtime overlaps with an active showtime on this screen");
        }
    }

    private Showtime getEntity(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
    }

    private void apply(Showtime showtime, ShowtimeRequest request, Movie movie, Screen screen, Instant endTime) {
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        showtime.setNormalPrice(request.normalPrice());
        showtime.setCouplePrice(request.couplePrice());
        showtime.setCleaningBufferMinutes(request.cleaningBufferMinutes());
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        return new ShowtimeResponse(showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                showtime.getScreen().getId(), showtime.getScreen().getName(),
                showtime.getScreen().getCinema().getId(), showtime.getScreen().getCinema().getName(),
                showtime.getStartTime(), showtime.getEndTime(), showtime.getCleaningBufferMinutes(),
                showtime.getNormalPrice(), showtime.getCouplePrice(), showtime.getStatus(),
                showtime.getCreatedAt(), showtime.getUpdatedAt());
    }
}
