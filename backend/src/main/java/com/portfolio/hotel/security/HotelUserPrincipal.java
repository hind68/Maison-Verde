package com.portfolio.hotel.security;

import com.portfolio.hotel.user.domain.StaffAccount;
import com.portfolio.hotel.user.domain.StaffRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record HotelUserPrincipal(StaffAccount account) implements UserDetails {

    public long id() { return account.id(); }
    public String email() { return account.email(); }
    public String firstName() { return account.firstName(); }
    public String lastName() { return account.lastName(); }
    public StaffRole role() { return account.role(); }
    public boolean mustChangePassword() { return account.mustChangePassword(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()));
    }
    @Override public String getPassword() { return account.passwordHash(); }
    @Override public String getUsername() { return account.login(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return account.active(); }
    @Override public boolean isAccountNonLocked() {
        return account.failedAttempts() < 5;
    }
}
