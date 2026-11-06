package com.portfolio.hotel.security;

import com.portfolio.hotel.user.repository.StaffAccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HotelUserDetailsService implements UserDetailsService {

    private final StaffAccountRepository repository;

    public HotelUserDetailsService(StaffAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String normalized = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        return repository.findByIdentifier(normalized).map(HotelUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides."));
    }
}
