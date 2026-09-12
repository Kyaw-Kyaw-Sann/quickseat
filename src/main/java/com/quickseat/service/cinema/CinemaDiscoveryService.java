package com.quickseat.service.cinema;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.entity.Cinema;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CinemaDiscoveryService {
    private final CinemaRepository cinemaRepository;

    @Transactional(readOnly = true)
    public PageResponse<CinemaResponse> list(String search, String city, int page, int size) {
        String normalizedSearch = normalize(search);
        String normalizedCity = normalize(city);

        Specification<Cinema> specification = (root, query, builder) -> builder.isTrue(root.get("active"));
        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase() + "%";
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("name")), pattern));
        }
        if (normalizedCity != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("city")), normalizedCity.toLowerCase()));
        }

        Page<CinemaResponse> result = cinemaRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id").ascending())))
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CinemaResponse get(Long cinemaId) {
        Cinema cinema = cinemaRepository.findByIdAndActiveTrue(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
        return toResponse(cinema);
    }

    private CinemaResponse toResponse(Cinema cinema) {
        return new CinemaResponse(cinema.getId(), cinema.getName(), cinema.getAddress(), cinema.getCity(),
                cinema.getPhone(), cinema.getImageUrl(), cinema.isActive(), cinema.getCreatedAt(),
                cinema.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
