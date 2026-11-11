package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.query.GetAccreditationHistoryQuery;
import com.willowwealth.accreditation.application.port.in.usecase.GetAccreditationHistoryUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationHistoryRepository;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.model.AccreditationHistory;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class GetAccreditationHistoryService implements GetAccreditationHistoryUseCase {

    private final AccreditationRepository repository;
    private final AccreditationHistoryRepository history;

    @Override
    public AccreditationHistory get(GetAccreditationHistoryQuery query) {
        var accreditation = repository.findById(query.accreditationId())
                .orElseThrow(() -> new AccreditationNotFoundException(query.accreditationId()));
        var changes = history.findByAccreditationId(accreditation.id());
        log.debug("Found {} changes for accreditation {}", changes.size(), accreditation.id());
        return new AccreditationHistory(accreditation.id(), accreditation.userId(), changes);
    }
}
