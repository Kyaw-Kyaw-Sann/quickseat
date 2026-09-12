package com.quickseat.service.showtime;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.showtime.ShowtimeDiscoveryResponse;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.repository.ShowtimeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowtimeDiscoveryService {
    private static final ZoneId MYANMAR_ZONE = ZoneId.of("Asia/Yangon");

    private final ShowtimeRepository showtimeRepository;
    private final Clock utcClock;

    @Transactional(readOnly = true)
    public PageResponse<ShowtimeDiscoveryResponse> list(Long movieId, Long cinemaId, LocalDate date,
                                                        int page, int size) {
        Instant now = Instant.now(utcClock);
        Specification<Showtime> specification = (root, query, builder) -> builder.and(
                builder.equal(root.get("status"), ShowtimeStatus.ACTIVE),
                builder.greaterThan(root.get("startTime"), now),
                builder.isTrue(root.get("movie").get("active")),
                builder.isTrue(root.get("screen").get("active")),
                builder.isTrue(root.get("screen").get("cinema").get("active")));

        if (movieId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movie").get("id"), movieId));
        }
        if (cinemaId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("screen").get("cinema").get("id"), cinemaId));
        }
        if (date != null) {
            Instant dayStart = date.atStartOfDay(MYANMAR_ZONE).toInstant();
            Instant nextDayStart = date.plusDays(1).atStartOfDay(MYANMAR_ZONE).toInstant();
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.greaterThanOrEqualTo(root.get("startTime"), dayStart),
                    builder.lessThan(root.get("startTime"), nextDayStart)));
        }

        Page<ShowtimeDiscoveryResponse> result = showtimeRepository.findAll(specification,
                        PageRequest.of(page, size,
                                Sort.by("startTime").ascending().and(Sort.by("id").ascending())))
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    private ShowtimeDiscoveryResponse toResponse(Showtime showtime) {
        return new ShowtimeDiscoveryResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getScreen().getCinema().getId(),
                showtime.getScreen().getCinema().getName(),
                showtime.getScreen().getId(),
                showtime.getScreen().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getNormalPrice(),
                showtime.getCouplePrice());
    }
}
