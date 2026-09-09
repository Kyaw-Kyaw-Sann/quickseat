package com.quickseat.service;

import com.quickseat.dto.ScreenRequest;
import com.quickseat.dto.ScreenResponse;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.Screen;
import com.quickseat.exception.BadRequestException;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
import com.quickseat.repository.ScreenRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenService {
    private final ScreenRepository screenRepository;
    private final CinemaRepository cinemaRepository;

    @Transactional
    public ScreenResponse create(Long cinemaId, ScreenRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
        if (!cinema.isActive()) throw new BadRequestException("Cannot add a screen to an inactive cinema");
        String name = request.name().trim();
        if (screenRepository.existsByCinemaIdAndNameIgnoreCase(cinemaId, name)) {
            throw new ConflictException("Screen name already exists in this cinema");
        }
        Screen screen = new Screen();
        screen.setCinema(cinema);
        screen.setName(name);
        Screen saved = screenRepository.save(screen);
        log.info("Screen created with id {} for cinema {}", saved.getId(), cinemaId);
        return toResponse(saved);
    }

    @Transactional
    public ScreenResponse update(Long id, ScreenRequest request) {
        Screen screen = getEntity(id);
        String name = request.name().trim();
        if (screenRepository.existsByCinemaIdAndNameIgnoreCaseAndIdNot(screen.getCinema().getId(), name, id)) {
            throw new ConflictException("Screen name already exists in this cinema");
        }
        screen.setName(name);
        return toResponse(screen);
    }

    public List<ScreenResponse> list(Long cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) throw new ResourceNotFoundException("Cinema not found");
        return screenRepository.findByCinemaIdOrderByNameAsc(cinemaId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScreenResponse setActive(Long id, boolean active) {
        Screen screen = getEntity(id);
        if (active && !screen.getCinema().isActive()) throw new BadRequestException("Cannot activate a screen in an inactive cinema");
        screen.setActive(active);
        log.info("Screen {} active status changed to {}", id, active);
        return toResponse(screen);
    }

    public Screen getEntity(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
    }

    private ScreenResponse toResponse(Screen screen) {
        return new ScreenResponse(screen.getId(), screen.getCinema().getId(), screen.getName(), screen.isActive(),
                screen.getCreatedAt(), screen.getUpdatedAt());
    }
}
