package com.quickseat.service.cinema;

import com.quickseat.dto.request.cinema.CinemaRequest;
import com.quickseat.dto.response.cinema.CinemaResponse;
import com.quickseat.entity.Cinema;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
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
public class CinemaService {
    private final CinemaRepository cinemaRepository;

    @Transactional
    public CinemaResponse create(CinemaRequest request) {
        Cinema cinema = new Cinema();
        apply(cinema, request);
        Cinema saved = cinemaRepository.save(cinema);
        log.info("Cinema created with id {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = getEntity(id);
        apply(cinema, request);
        return toResponse(cinema);
    }

    public CinemaResponse get(Long id) { return toResponse(getEntity(id)); }

    public Page<CinemaResponse> list(String search, Boolean active, int page, int size) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Specification<Cinema> specification = (root, query, builder) -> builder.conjunction();
        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("city")), pattern)));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        return cinemaRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toResponse);
    }

    @Transactional
    public CinemaResponse setActive(Long id, boolean active) {
        Cinema cinema = getEntity(id);
        cinema.setActive(active);
        log.info("Cinema {} active status changed to {}", id, active);
        return toResponse(cinema);
    }

    public Cinema getEntity(Long id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
    }

    private void apply(Cinema cinema, CinemaRequest request) {
        cinema.setName(request.name().trim());
        cinema.setAddress(request.address().trim());
        cinema.setCity(request.city().trim());
        cinema.setPhone(trimToNull(request.phone()));
        cinema.setImageUrl(trimToNull(request.imageUrl()));
    }

    private CinemaResponse toResponse(Cinema cinema) {
        return new CinemaResponse(cinema.getId(), cinema.getName(), cinema.getAddress(), cinema.getCity(),
                cinema.getPhone(), cinema.getImageUrl(), cinema.isActive(), cinema.getCreatedAt(), cinema.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
