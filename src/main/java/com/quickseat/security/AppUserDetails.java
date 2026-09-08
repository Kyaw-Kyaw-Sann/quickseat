package com.quickseat.security;

import com.quickseat.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserDetails(Long id, String username, String password, boolean enabled,
        Collection<? extends GrantedAuthority> authorities) implements UserDetails {
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(user.getId(), user.getEmail(), user.getPassword(), user.isActive(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
