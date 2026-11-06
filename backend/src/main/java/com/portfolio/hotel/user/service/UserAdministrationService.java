package com.portfolio.hotel.user.service;

import com.portfolio.hotel.security.HotelUserPrincipal;
import com.portfolio.hotel.shared.dto.PagedResponse;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import com.portfolio.hotel.shared.error.ResourceConflictException;
import com.portfolio.hotel.shared.error.ResourceNotFoundException;
import com.portfolio.hotel.user.domain.StaffAccount;
import com.portfolio.hotel.user.domain.StaffRole;
import com.portfolio.hotel.user.dto.CreateUserRequest;
import com.portfolio.hotel.user.dto.ResetPasswordRequest;
import com.portfolio.hotel.user.dto.UpdateUserRequest;
import com.portfolio.hotel.user.dto.UserResponse;
import com.portfolio.hotel.user.repository.StaffAccountRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdministrationService {

    private final StaffAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAdministrationService(StaffAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(String query, int page, int size) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return repository.findAll(normalized, page, size);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        return createAccount(request, true);
    }

    @Transactional
    public UserResponse update(long id, UpdateUserRequest request, HotelUserPrincipal actor) {
        StaffAccount target = repository.lockById(id);
        if (target.id() == actor.id()
                && (!target.role().equals(request.role()) || !request.active())) {
            throw new BusinessValidationException(
                    "Vous ne pouvez pas modifier votre propre rôle ni désactiver votre compte.");
        }
        boolean removesActiveAdmin = target.active() && target.role() == StaffRole.ADMIN
                && (!request.active() || request.role() != StaffRole.ADMIN);
        if (removesActiveAdmin && repository.countActiveAdmins() <= 1) {
            throw new ResourceConflictException("Le dernier administrateur actif doit être conservé.");
        }
        repository.updateRoleAndActive(id, request.role(), request.active());
        return response(id);
    }

    @Transactional
    public UserResponse resetPassword(long id, ResetPasswordRequest request) {
        repository.lockById(id);
        repository.updatePassword(id, passwordEncoder.encode(request.password()), true);
        return response(id);
    }

    @Transactional
    public void createBootstrapAdmin(
            String login, String email, String password, String firstName, String lastName
    ) {
        if (repository.countActiveAdmins() > 0) {
            return;
        }
        createAccount(new CreateUserRequest(
                login, email, firstName, lastName, StaffRole.ADMIN, password), false);
    }

    private UserResponse createAccount(CreateUserRequest request, boolean mustChangePassword) {
        String login = normalizeIdentifier(request.login());
        String email = normalizeIdentifier(request.email());
        if (login.contains("@")) {
            throw new BusinessValidationException("Le login ne doit pas être une adresse e-mail.");
        }
        try {
            long id = repository.create(
                    login, email, passwordEncoder.encode(request.initialPassword()),
                    request.firstName().trim(), request.lastName().trim(), request.role(), mustChangePassword);
            return response(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Ce login ou cette adresse e-mail est déjà utilisé.");
        }
    }

    private UserResponse response(long id) {
        return repository.findById(id).map(account -> new UserResponse(
                account.id(), account.login(), account.email(), account.firstName(), account.lastName(),
                account.role(), account.active(), account.mustChangePassword()
        )).orElseThrow(() -> new ResourceNotFoundException("Le compte demandé n'existe pas."));
    }

    private String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
