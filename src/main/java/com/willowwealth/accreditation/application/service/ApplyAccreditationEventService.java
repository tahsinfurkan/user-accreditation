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
        // The API checks this as well, but two submissions of the same user can pass that check at the same time.
        // A rejection is not a failure: the event is acknowledged and never retried.
        if (repository.existsPendingFor(event.userId())) {
            log.warn("Rejected accreditation {} of user {}: the user already has a pending accreditation",
                    event.accreditationId(), event.userId());
            return;
        }
        repository.insert(Accreditation.from(event));
        log.info("Accreditation {} recorded as PENDING for user {}", event.accreditationId(), event.userId());
    }
}
