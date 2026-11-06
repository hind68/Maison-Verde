package com.portfolio.hotel.availability.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.portfolio.hotel.availability.repository.AvailabilityRepository;
import com.portfolio.hotel.shared.error.BusinessValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AvailabilityServiceTest {

    private final AvailabilityRepository repository = mock(AvailabilityRepository.class);
    private final AvailabilityService service = new AvailabilityService(repository);

    @Test
    void rejectsZeroNightStayBeforeQueryingOracle() {
        LocalDate date = LocalDate.of(2040, 1, 1);

        assertThatThrownBy(() -> service.search(date, date, 1, 0, null))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(repository);
    }
}
