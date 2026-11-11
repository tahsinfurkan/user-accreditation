package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.usecase.ApplyAccreditationEventUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationHistoryRepository;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.StatusChange;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Rejections below are not failures: the event is acknowledged and never retried.
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ApplyAccreditationEventService implements ApplyAccreditationEventUseCase {

    private final AccreditationRepository repository;
    private final AccreditationHistoryRepository history;

    @Override
    @Transactional
    public void apply(AccreditationEvent event) {
        // Kafka delivers at least once, so the same event can arrive twice. Every applied event is in the history.
        if (history.contains(event.eventId())) {
            log.debug("Event {} of accreditation {} was already applied, skipping it", event.eventId(),
                    event.accreditationId());
            return;
        }
        switch (event) {
            case AccreditationSubmitted submitted -> record(submitted);
            case AccreditationStatusChanged change -> changeStatus(change);
        }
    }

    private void record(AccreditationSubmitted event) {
        // The API checks this as well, but two submissions of the same user can pass that check at the same time.
        if (repository.existsPendingFor(event.userId())) {
            log.warn("Rejected accreditation {} of user {}: the user already has a pending accreditation",
                    event.accreditationId(), event.userId());
            return;
        }
        repository.insert(Accreditation.from(event));
        history.append(StatusChange.from(event));
        log.info("Accreditation {} recorded as PENDING for user {}", event.accreditationId(), event.userId());
    }

    private void changeStatus(AccreditationStatusChanged event) {
        var accreditation = repository.findById(event.accreditationId());
        if (accreditation.isEmpty()) {
            log.warn("Rejected status change {}: accreditation {} does not exist", event.eventId(), event.accreditationId());
            return;
        }
        try {
            repository.update(accreditation.get().apply(event));
        } catch (InvalidStatusTransitionException e) {
            // Usually two admins decided on the same accreditation and the other change got here first.
            log.warn("Rejected status change {} of accreditation {}: it was decided for {} -> {} but the status is {}",
                    event.eventId(), event.accreditationId(), event.previousStatus(), event.newStatus(),
                    accreditation.get().status());
            return;
        }
        history.append(StatusChange.from(event));
        log.info("Accreditation {} of user {} changed from {} to {}", event.accreditationId(), event.userId(),
                event.previousStatus(), event.newStatus());
    }
}
