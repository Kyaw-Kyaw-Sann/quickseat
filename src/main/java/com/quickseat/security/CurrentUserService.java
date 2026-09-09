package com.quickseat.security;

import com.quickseat.exception.UnauthorizedException;
import com.quickseat.entity.enums.Role;
import com.quickseat.entity.User;
import com.quickseat.exception.ForbiddenException;
import com.quickseat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor public class CurrentUserService {
    private final UserRepository userRepository;
    public AppUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails user)) throw new UnauthorizedException("Authentication is required");
        return user;
    }

    public User getVerifiedCustomer() {
        AppUserDetails principal = getCurrentUser();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        if (user.getRole() != Role.CUSTOMER) throw new ForbiddenException("Customer access is required");
        if (!user.isEmailVerified()) throw new ForbiddenException("Email verification is required");
        return user;
    }
}
