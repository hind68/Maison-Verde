package com.portfolio.hotel.client.controller;

import com.portfolio.hotel.client.dto.ClientSummaryResponse;
import com.portfolio.hotel.client.dto.CreateClientRequest;
import com.portfolio.hotel.client.service.ClientService;
import com.portfolio.hotel.shared.dto.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public PagedResponse<ClientSummaryResponse> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return clientService.search(query, page, size);
    }

    @PostMapping
    public org.springframework.http.ResponseEntity<ClientSummaryResponse> create(
            @Valid @RequestBody CreateClientRequest request
    ) {
        return org.springframework.http.ResponseEntity.status(201).body(clientService.create(request));
    }
}
