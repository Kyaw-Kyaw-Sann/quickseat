package com.quickseat.config;

import com.quickseat.entity.Cinema;
import com.quickseat.entity.Movie;
import com.quickseat.entity.Screen;
import com.quickseat.entity.Seat;
import com.quickseat.entity.Showtime;
import com.quickseat.entity.ShowtimeSeat;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.entity.enums.MovieStatus;
import com.quickseat.entity.enums.Role;
import com.quickseat.entity.enums.SeatInventoryStatus;
import com.quickseat.entity.enums.SeatType;
import com.quickseat.entity.enums.ShowtimeStatus;
import com.quickseat.repository.CinemaRepository;
import com.quickseat.repository.MovieRepository;
import com.quickseat.repository.ScreenRepository;
import com.quickseat.repository.SeatRepository;
import com.quickseat.repository.ShowtimeRepository;
import com.quickseat.repository.ShowtimeSeatRepository;
import com.quickseat.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a small, predictable data set for local development only. It is opt-in through
 * DEMO_SEED_ENABLED and never loads under the production profile.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final String CINEMA_NAME = "QuickSeat Demo Cinema";
    private static final String ADMIN_EMAIL = "demo.admin@quickseat.local";
    private static final String STAFF_EMAIL = "demo.staff@quickseat.local";
    private static final String CUSTOMER_EMAIL = "demo.customer@quickseat.local";

    private final CinemaRepository cinemaRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-seed.password}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Cinema cinema = cinemaRepository.findAll().stream()
                .filter(item -> CINEMA_NAME.equals(item.getName()))
                .findFirst()
                .orElseGet(this::createCinema);

        Screen screen = screenRepository.findByCinemaIdOrderByNameAsc(cinema.getId()).stream()
                .filter(item -> "Screen 1".equals(item.getName()))
                .findFirst()
                .orElseGet(() -> createScreen(cinema));

        List<Seat> seats = seatRepository.findByScreenIdOrderByRowNameAscSeatNumberAsc(screen.getId());
        if (seats.isEmpty()) {
            seats = createSeats(screen);
        }

        Movie movie = movieRepository.findAll().stream()
                .filter(item -> "QuickSeat Demo Feature".equals(item.getTitle()))
                .findFirst()
                .orElseGet(this::createMovie);

        createFutureShowtimeIfMissing(movie, screen, seats);
        createUserIfMissing("Demo Admin", ADMIN_EMAIL, Role.ADMIN, null);
        createUserIfMissing("Demo Staff", STAFF_EMAIL, Role.STAFF, cinema);
        createUserIfMissing("Demo Customer", CUSTOMER_EMAIL, Role.CUSTOMER, null);

        log.info("Development demo data is ready: cinema={}, users={}", cinema.getName(), 3);
    }

    private Cinema createCinema() {
        Cinema cinema = new Cinema();
        cinema.setName(CINEMA_NAME);
        cinema.setAddress("123 Demo Road");
        cinema.setCity("Yangon");
        cinema.setPhone("09123456789");
        cinema.setActive(true);
        return cinemaRepository.save(cinema);
    }

    private Screen createScreen(Cinema cinema) {
        Screen screen = new Screen();
        screen.setCinema(cinema);
        screen.setName("Screen 1");
        screen.setActive(true);
        return screenRepository.save(screen);
    }

    private List<Seat> createSeats(Screen screen) {
        List<Seat> seats = new ArrayList<>();
        for (int number = 1; number <= 5; number++) {
            seats.add(createSeat(screen, "A", number, SeatType.NORMAL));
        }
        for (int number = 1; number <= 2; number++) {
            seats.add(createSeat(screen, "B", number, SeatType.COUPLE));
        }
        return seatRepository.saveAll(seats);
    }

    private Seat createSeat(Screen screen, String rowName, int seatNumber, SeatType seatType) {
        Seat seat = new Seat();
        seat.setScreen(screen);
        seat.setRowName(rowName);
        seat.setSeatNumber(seatNumber);
        seat.setSeatType(seatType);
        seat.setActive(true);
        return seat;
    }

    private Movie createMovie() {
        Movie movie = new Movie();
        movie.setTitle("QuickSeat Demo Feature");
        movie.setDescription("Development-only sample movie for testing the QuickSeat flow.");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.now());
        movie.setLanguage("English");
        movie.setGenres(new String[]{"Drama", "Demo"});
        movie.setAgeRating("PG-13");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    private void createFutureShowtimeIfMissing(Movie movie, Screen screen, List<Seat> seats) {
        boolean exists = showtimeRepository.findAll().stream()
                .anyMatch(item -> item.getMovie().getId().equals(movie.getId())
                        && item.getScreen().getId().equals(screen.getId())
                        && item.getStatus() == ShowtimeStatus.ACTIVE
                        && item.getStartTime().isAfter(Instant.now()));
        if (exists) {
            return;
        }

        Instant startTime = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(startTime);
        showtime.setEndTime(startTime.plus(135, ChronoUnit.MINUTES));
        showtime.setCleaningBufferMinutes(15);
        showtime.setNormalPrice(new BigDecimal("5000.00"));
        showtime.setCouplePrice(new BigDecimal("9000.00"));
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        Showtime savedShowtime = showtimeRepository.save(showtime);

        List<ShowtimeSeat> inventory = seats.stream().map(seat -> {
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(savedShowtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus(SeatInventoryStatus.AVAILABLE);
            return showtimeSeat;
        }).toList();
        showtimeSeatRepository.saveAll(inventory);
    }

    private void createUserIfMissing(String name, String email, Role role, Cinema cinema) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setRole(role);
        user.setCinema(cinema);
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(true);
        user.setActive(true);
        userRepository.save(user);
    }
}
