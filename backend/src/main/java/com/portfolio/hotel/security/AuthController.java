package com.portfolio.hotel.security;

import com.portfolio.hotel.security.dto.AuthenticatedUserResponse;
import com.portfolio.hotel.security.dto.ChangePasswordRequest;
import com.portfolio.hotel.security.dto.CsrfResponse;
import com.portfolio.hotel.security.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final SecurityContextRepository contextRepository;

    public AuthController(
            AuthenticationService authenticationService,
            SecurityContextRepository contextRepository
    ) {
        this.authenticationService = authenticationService;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/login")
    public AuthenticatedUserResponse login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Authentication authentication = authenticationService.authenticate(
                request.identifier(), request.password());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return AuthenticatedUserResponse.from((HotelUserPrincipal) authentication.getPrincipal());
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal HotelUserPrincipal principal) {
        return AuthenticatedUserResponse.from(principal);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal HotelUserPrincipal principal,
            @RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse,
            Authentication authentication
    ) {
        authenticationService.changePassword(principal, request);
        new SecurityContextLogoutHandler().logout(servletRequest, servletResponse, authentication);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
