package com.quickseat.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.quickseat.entity.Cinema;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.Role;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.exception.UnauthorizedException;
import com.quickseat.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {
    @Mock UserRepository userRepository;
    private CurrentUserService service;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new CurrentUserService(userRepository);
        AppUserDetails principal = new AppUserDetails(1L, "staff@example.com", "password", true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Cinema cinema = new Cinema();
        cinema.setId(2L);
        cinema.setActive(true);
        staff = new User();
        staff.setId(1L);
        staff.setRole(Role.STAFF);
        staff.setActive(true);
        staff.setCinema(cinema);
        when(userRepository.findById(1L)).thenReturn(Optional.of(staff));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsActiveStaffWithAssignedCinema() {
        assertThat(service.getActiveStaffWithCinema()).isSameAs(staff);
    }

    @Test
    void disabledStaffIsRejected() {
        staff.setActive(false);

        assertThatThrownBy(service::getActiveStaffWithCinema)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void unassignedStaffIsRejected() {
        staff.setCinema(null);

        assertThatThrownBy(service::getActiveStaffWithCinema)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not assigned");
    }

    @Test
    void staffAssignedToInactiveCinemaIsRejected() {
        staff.getCinema().setActive(false);

        assertThatThrownBy(service::getActiveStaffWithCinema)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void nonStaffRoleIsRejected() {
        staff.setRole(Role.CUSTOMER);

        assertThatThrownBy(service::getActiveStaffWithCinema)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Staff access");
    }
}
