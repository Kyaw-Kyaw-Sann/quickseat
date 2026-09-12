package com.quickseat.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickseat.dto.request.user.StaffCreateRequest;
import com.quickseat.dto.request.user.StaffUpdateRequest;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.entity.enums.Role;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
import com.quickseat.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserRepository userRepository;
    @Mock CinemaRepository cinemaRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminUserService service;
    private Cinema cinema;
    private User customer;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, cinemaRepository, passwordEncoder);
        cinema = new Cinema();
        cinema.setId(1L);
        cinema.setName("Cinema One");
        cinema.setActive(true);

        customer = new User();
        customer.setId(2L);
        customer.setName("Customer");
        customer.setEmail("customer@example.com");
        customer.setRole(Role.CUSTOMER);
        customer.setProvider(AuthProvider.LOCAL);
        customer.setActive(true);

        staff = new User();
        staff.setId(3L);
        staff.setName("Staff");
        staff.setEmail("staff@example.com");
        staff.setPassword("existing-hash");
        staff.setRole(Role.STAFF);
        staff.setProvider(AuthProvider.LOCAL);
        staff.setCinema(cinema);
        staff.setActive(true);
    }

    @Test
    void createsStaffWithHashedPasswordAndActiveCinema() {
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var result = service.createStaff(new StaffCreateRequest(
                " New Staff ", "STAFF2@EXAMPLE.COM", "password123", " 0912345 ", 1L));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.email()).isEqualTo("staff2@example.com");
        assertThat(result.cinemaId()).isEqualTo(1L);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getRole() == Role.STAFF && user.isEmailVerified()
                        && "bcrypt-hash".equals(user.getPassword())));
    }

    @Test
    void rejectsStaffCreationForInactiveCinema() {
        cinema.setActive(false);
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));

        assertThatThrownBy(() -> service.createStaff(new StaffCreateRequest(
                "Staff", "new@example.com", "password123", null, 1L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active cinema");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void rejectsDuplicateStaffEmail() {
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createStaff(new StaffCreateRequest(
                "Staff", "STAFF@example.com", "password123", null, 1L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatesStaffAndKeepsPasswordWhenNotProvided() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));

        var result = service.updateStaff(3L,
                new StaffUpdateRequest("Updated Staff", "updated@example.com", null, "099"));

        assertThat(result.name()).isEqualTo("Updated Staff");
        assertThat(staff.getPassword()).isEqualTo("existing-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateStaffHashesNewPassword() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-hash");

        service.updateStaff(3L,
                new StaffUpdateRequest("Staff", "staff@example.com", "newpassword", null));

        assertThat(staff.getPassword()).isEqualTo("new-hash");
    }

    @Test
    void assignsStaffToActiveCinema() {
        Cinema secondCinema = new Cinema();
        secondCinema.setId(9L);
        secondCinema.setName("Cinema Two");
        secondCinema.setActive(true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(staff));
        when(cinemaRepository.findById(9L)).thenReturn(Optional.of(secondCinema));

        var result = service.assignCinema(3L, 9L);

        assertThat(result.cinemaId()).isEqualTo(9L);
    }

    @Test
    void customerActivationDoesNotChangeRoleOrCreateNewRecord() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));

        var result = service.setCustomerActive(2L, false);

        assertThat(result.active()).isFalse();
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(customer.getCinema()).isNull();
        verify(userRepository, never()).deleteById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void wrongRoleCannotBeManagedAsStaff() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.getStaff(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customerAndStaffListsSupportFilteringAndPagination() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)))
                .thenReturn(new PageImpl<>(List.of(staff)));

        var customers = service.listCustomers("customer", true, false, 0, 20);
        var staffAccounts = service.listStaff("staff", true, 1L, 0, 20);

        assertThat(customers.content()).extracting("email").containsExactly("customer@example.com");
        assertThat(staffAccounts.content()).extracting("cinemaId").containsExactly(1L);
    }
}
