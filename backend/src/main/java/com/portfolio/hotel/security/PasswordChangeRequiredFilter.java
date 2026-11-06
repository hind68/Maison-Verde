package com.portfolio.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.hotel.shared.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of(
            "/api/v1/auth/me", "/api/v1/auth/logout", "/api/v1/auth/change-password", "/api/v1/auth/csrf"
    );
    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof HotelUserPrincipal principal
                && principal.mustChangePassword() && !ALLOWED.contains(request.getRequestURI())) {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(
                    403, "PASSWORD_CHANGE_REQUIRED", "Le mot de passe initial doit être remplacé."));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
