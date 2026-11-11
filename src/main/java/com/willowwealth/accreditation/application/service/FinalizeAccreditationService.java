package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.command.FinalizeAccreditationCommand;
import com.willowwealth.accreditation.application.port.in.usecase.FinalizeAccreditationUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class FinalizeAccreditationService implements FinalizeAccreditationUseCase {

    private final AccreditationRepository repository;
    private final AccreditationEventPublisher eventPublisher;
    private final Clock clock;

    // Like a submission, this only publishes the change; the consumer checks it again before writing it.
    @Override
    public void finalizeAccreditation(FinalizeAccreditationCommand command) {
        var accreditation = repository.findById(command.accreditationId())
                .orElseThrow(() -> new AccreditationNotFoundException(command.accreditationId()));
        var change = accreditation.changeStatus(command.outcome(), ChangeOrigin.ADMIN, clock.instant());

        eventPublisher.publish(change);
        log.info("Accreditation {} of user {} is changing from {} to {}", change.accreditationId(), change.userId(),
                change.previousStatus(), change.newStatus());
    }
}
