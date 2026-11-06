package com.portfolio.hotel.user.controller;

import com.portfolio.hotel.security.HotelUserPrincipal;
import com.portfolio.hotel.shared.dto.PagedResponse;
import com.portfolio.hotel.user.dto.CreateUserRequest;
import com.portfolio.hotel.user.dto.ResetPasswordRequest;
import com.portfolio.hotel.user.dto.UpdateUserRequest;
import com.portfolio.hotel.user.dto.UserResponse;
import com.portfolio.hotel.user.service.UserAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserAdministrationController {

    private final UserAdministrationService service;

    public UserAdministrationController(UserAdministrationService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<UserResponse> findAll(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.findAll(query, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid CreateUserRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}")
    public UserResponse update(
            @PathVariable long id,
            @RequestBody @Valid UpdateUserRequest request,
            @AuthenticationPrincipal HotelUserPrincipal actor
    ) {
        return service.update(id, request, actor);
    }

    @PatchMapping("/{id}/password")
    public UserResponse resetPassword(
            @PathVariable long id, @RequestBody @Valid ResetPasswordRequest request
    ) {
        return service.resetPassword(id, request);
    }
}
