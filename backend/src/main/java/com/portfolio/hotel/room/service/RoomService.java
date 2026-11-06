package com.portfolio.hotel.room.service;

import com.portfolio.hotel.room.dto.RoomCatalogResponse;
import com.portfolio.hotel.room.repository.RoomCatalogRepository;
import com.portfolio.hotel.room.repository.RoomOperationsRepository;
import com.portfolio.hotel.shared.dto.PagedResponse;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private static final Set<String> STATUSES = Set.of("en_service", "hors_service");
    private static final Set<String> HOUSEKEEPING_STATUSES = Set.of("a_nettoyer", "nettoyee", "prete");
    private final RoomCatalogRepository repository;
    private final RoomOperationsRepository operationsRepository;

    public RoomService(RoomCatalogRepository repository, RoomOperationsRepository operationsRepository) {
        this.repository = repository;
        this.operationsRepository = operationsRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<RoomCatalogResponse> findAll(
            String query, String status, String housekeepingStatus, Integer floor, int minCapacity, int page, int size
    ) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        String normalizedHousekeeping = housekeepingStatus == null
                ? "" : housekeepingStatus.trim().toLowerCase(Locale.ROOT);
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) {
            throw new BusinessValidationException("Le statut opérationnel demandé est invalide.");
        }
        if (!normalizedHousekeeping.isEmpty() && !HOUSEKEEPING_STATUSES.contains(normalizedHousekeeping)) {
            throw new BusinessValidationException("L'état de propreté demandé est invalide.");
        }
        return repository.findAll(
                normalizedQuery, normalizedStatus, normalizedHousekeeping, floor, minCapacity, page, size);
    }

    @Transactional
    public RoomCatalogResponse updateHousekeeping(long id, String status) {
        String normalized = normalize(status, HOUSEKEEPING_STATUSES, "L'état de propreté demandé est invalide.");
        operationsRepository.updateHousekeeping(id, normalized);
        return requireRoom(id);
    }

    @Transactional
    public RoomCatalogResponse updateOperationalStatus(long id, String status) {
        String normalized = normalize(status, STATUSES, "Le statut opérationnel demandé est invalide.");
        operationsRepository.updateOperationalStatus(id, normalized);
        return requireRoom(id);
    }

    private RoomCatalogResponse requireRoom(long id) {
        RoomCatalogResponse room = repository.findById(id);
        if (room == null) {
            throw new com.portfolio.hotel.shared.error.ResourceNotFoundException(
                    "La chambre demandée n'existe pas.");
        }
        return room;
    }

    private String normalize(String value, Set<String> allowed, String message) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessValidationException(message);
        }
        return normalized;
    }
}
