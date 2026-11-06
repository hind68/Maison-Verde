package com.portfolio.hotel.security;

import com.portfolio.hotel.security.dto.ChangePasswordRequest;
import com.portfolio.hotel.shared.error.AuthenticationFailedException;
import com.portfolio.hotel.user.repository.StaffAccountRepository;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final StaffAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            StaffAccountRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public Authentication authenticate(String identifier, String password) {
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalized, password));
            HotelUserPrincipal principal = (HotelUserPrincipal) authentication.getPrincipal();
            repository.recordLoginSuccess(principal.id());
            return authentication;
        } catch (AuthenticationException exception) {
            repository.recordLoginFailure(normalized);
            throw new AuthenticationFailedException("Identifiant ou mot de passe incorrect, ou compte indisponible.");
        }
    }

    @Transactional
    public void changePassword(HotelUserPrincipal principal, ChangePasswordRequest request) {
        var account = repository.findById(principal.id())
                .orElseThrow(() -> new AuthenticationFailedException("La session n'est plus valide."));
        if (!passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            throw new AuthenticationFailedException("Le mot de passe actuel est incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), account.passwordHash())) {
            throw new com.portfolio.hotel.shared.error.BusinessValidationException(
                    "Le nouveau mot de passe doit être différent du mot de passe actuel.");
        }
        repository.updatePassword(principal.id(), passwordEncoder.encode(request.newPassword()), false);
    }
}
