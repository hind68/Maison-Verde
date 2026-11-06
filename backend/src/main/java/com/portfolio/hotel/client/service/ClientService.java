package com.portfolio.hotel.client.service;

import com.portfolio.hotel.client.domain.ClientEntity;
import com.portfolio.hotel.client.dto.ClientSummaryResponse;
import com.portfolio.hotel.client.dto.CreateClientRequest;
import com.portfolio.hotel.client.repository.ClientRepository;
import com.portfolio.hotel.shared.dto.PagedResponse;
import com.portfolio.hotel.shared.error.ResourceConflictException;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClientSummaryResponse> search(String query, int page, int size) {
        String normalized = query == null || query.isBlank() ? null : query.trim().toLowerCase(Locale.ROOT);
        Page<ClientEntity> result = clientRepository.search(
                normalized,
                PageRequest.of(page, size, Sort.by("lastName").ascending()
                        .and(Sort.by("firstName").ascending())
                        .and(Sort.by("id").ascending()))
        );
        return new PagedResponse<>(
                result.map(this::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public boolean exists(long id) {
        return clientRepository.existsById(id);
    }

    @Transactional
    public ClientSummaryResponse create(CreateClientRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (clientRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Un client utilise déjà cette adresse e-mail.");
        }
        ClientEntity client = new ClientEntity(
                request.lastName().trim(),
                request.firstName().trim(),
                normalizeOptional(request.phone()),
                email
        );
        try {
            return toResponse(clientRepository.saveAndFlush(client));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Un client utilise déjà cette adresse e-mail.");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ClientSummaryResponse toResponse(ClientEntity client) {
        return new ClientSummaryResponse(
                client.getId(), client.getFirstName(), client.getLastName(), client.getEmail(), client.getPhone()
        );
    }
}
