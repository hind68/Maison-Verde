package com.portfolio.hotel.planning.service;

import com.portfolio.hotel.planning.dto.PlanningResponse;
import com.portfolio.hotel.planning.repository.PlanningRepository;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningService {

    private final PlanningRepository repository;

    public PlanningService(PlanningRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PlanningResponse find(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw new BusinessValidationException("La fin du planning doit être postérieure à son début.");
        }
        if (ChronoUnit.DAYS.between(start, end) > 62) {
            throw new BusinessValidationException("Le planning est limité à 62 nuits par consultation.");
        }
        return repository.find(start, end);
    }
}
