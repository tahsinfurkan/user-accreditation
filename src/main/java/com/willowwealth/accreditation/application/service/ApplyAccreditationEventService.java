package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.usecase.ApplyAccreditationEventUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.model.Accreditation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApplyAccreditationEventService implements ApplyAccreditationEventUseCase {

    private final AccreditationRepository repository;

    @Override
    @Transactional
    public void apply(AccreditationSubmitted event) {
        // Kafka delivers at least once, so the same submission can arrive twice.
        if (repository.exists(event.accreditationId())) {
            log.debug("Accreditation {} is already recorded, skipping event {}", event.accreditationId(), event.eventId());
            return;
        }
        repository.insert(Accreditation.from(event));
        log.info("Accreditation {} recorded as PENDING for user {}", event.accreditationId(), event.userId());
    }
}
