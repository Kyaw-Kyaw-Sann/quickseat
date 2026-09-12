package com.quickseat.service.user;

import com.quickseat.dto.common.PageResponse;
import com.quickseat.dto.request.user.StaffCreateRequest;
import com.quickseat.dto.request.user.StaffUpdateRequest;
import com.quickseat.dto.response.user.CustomerResponse;
import com.quickseat.dto.response.user.StaffResponse;
import com.quickseat.entity.Cinema;
import com.quickseat.entity.User;
import com.quickseat.entity.enums.AuthProvider;
import com.quickseat.entity.enums.Role;
import com.quickseat.exception.ConflictException;
import com.quickseat.exception.ResourceNotFoundException;
import com.quickseat.repository.CinemaRepository;
import com.quickseat.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final CinemaRepository cinemaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> listCustomers(String search, Boolean active,
                                                         Boolean emailVerified, int page, int size) {
        Specification<User> specification = roleSpecification(Role.CUSTOMER);
        specification = addSearch(specification, search);
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (emailVerified != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("emailVerified"), emailVerified));
        }
        Page<CustomerResponse> response = userRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toCustomerResponse);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long userId) {
        return toCustomerResponse(getUserByRole(userId, Role.CUSTOMER, "Customer not found"));
    }

    @Transactional
    public CustomerResponse setCustomerActive(Long userId, boolean active) {
        User customer = getUserByRole(userId, Role.CUSTOMER, "Customer not found");
        customer.setActive(active);
        customer.setCinema(null);
        log.info("Customer {} active status changed to {}", customer.getId(), active);
        return toCustomerResponse(customer);
    }

    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        Cinema cinema = getActiveCinema(request.cinemaId());
        User staff = new User();
        staff.setName(request.name().trim());
        staff.setEmail(email);
        staff.setPassword(passwordEncoder.encode(request.password()));
        staff.setPhone(trimToNull(request.phone()));
        staff.setRole(Role.STAFF);
        staff.setCinema(cinema);
        staff.setProvider(AuthProvider.LOCAL);
        staff.setEmailVerified(true);
        staff.setActive(true);
        User saved = userRepository.save(staff);
        log.info("Staff account {} created for cinema {}", saved.getId(), cinema.getId());
        return toStaffResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> listStaff(String search, Boolean active, Long cinemaId,
                                                  int page, int size) {
        Specification<User> specification = roleSpecification(Role.STAFF);
        specification = addSearch(specification, search);
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (cinemaId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("cinema").get("id"), cinemaId));
        }
        Page<StaffResponse> response = userRepository.findAll(specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toStaffResponse);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(Long staffId) {
        return toStaffResponse(getUserByRole(staffId, Role.STAFF, "Staff not found"));
    }

    @Transactional
    public StaffResponse updateStaff(Long staffId, StaffUpdateRequest request) {
        User staff = getUserByRole(staffId, Role.STAFF, "Staff not found");
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndIdNot(email, staffId)) {
            throw new ConflictException("Email is already registered");
        }
        staff.setName(request.name().trim());
        staff.setEmail(email);
        staff.setPhone(trimToNull(request.phone()));
        if (request.password() != null && !request.password().isBlank()) {
            staff.setPassword(passwordEncoder.encode(request.password()));
        }
        enforceStaffInvariant(staff);
        log.info("Staff account {} updated", staff.getId());
        return toStaffResponse(staff);
    }

    @Transactional
    public StaffResponse setStaffActive(Long staffId, boolean active) {
        User staff = getUserByRole(staffId, Role.STAFF, "Staff not found");
        if (active) enforceStaffInvariant(staff);
        staff.setActive(active);
        log.info("Staff {} active status changed to {}", staff.getId(), active);
        return toStaffResponse(staff);
    }

    @Transactional
    public StaffResponse assignCinema(Long staffId, Long cinemaId) {
        User staff = getUserByRole(staffId, Role.STAFF, "Staff not found");
        staff.setCinema(getActiveCinema(cinemaId));
        log.info("Staff {} assigned to cinema {}", staff.getId(), cinemaId);
        return toStaffResponse(staff);
    }

    private Specification<User> roleSpecification(Role role) {
        return (root, query, builder) -> builder.equal(root.get("role"), role);
    }

    private Specification<User> addSearch(Specification<User> specification, String search) {
        if (search == null || search.isBlank()) return specification;
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return specification.and((root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("email")), pattern)));
    }

    private User getUserByRole(Long id, Role role, String notFoundMessage) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
        if (user.getRole() != role) throw new ResourceNotFoundException(notFoundMessage);
        return user;
    }

    private Cinema getActiveCinema(Long cinemaId) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cinema not found"));
        if (!cinema.isActive()) throw new ConflictException("Staff must be assigned to an active cinema");
        return cinema;
    }

    private void enforceStaffInvariant(User staff) {
        if (staff.getRole() != Role.STAFF || staff.getCinema() == null) {
            throw new ConflictException("Staff must be assigned to a cinema");
        }
        if (!staff.getCinema().isActive()) {
            throw new ConflictException("Staff must be assigned to an active cinema");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CustomerResponse toCustomerResponse(User user) {
        return new CustomerResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getProvider(), user.isEmailVerified(), user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private StaffResponse toStaffResponse(User user) {
        Cinema cinema = user.getCinema();
        return new StaffResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getProvider(),
                user.isActive(), cinema == null ? null : cinema.getId(), cinema == null ? null : cinema.getName(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
