package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.command.SubmitAccreditationCommand;
import com.willowwealth.accreditation.application.port.in.usecase.SubmitAccreditationUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.application.port.out.DocumentRepository;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubmitAccreditationService implements SubmitAccreditationUseCase {

    private final DocumentRepository documentRepository;
    private final AccreditationEventPublisher eventPublisher;
    private final Clock clock;

    // No transaction here on purpose: the document repository commits on its own,
    // so no database connection is held while waiting for Kafka.
    @Override
    public AccreditationId submit(SubmitAccreditationCommand command) {
        var id = AccreditationId.newId();
        var document = documentRepository.save(command.document());

        eventPublisher.publish(Accreditation.submit(id, command.userId(), command.type(), document, clock.instant()));
        log.info("Accreditation {} submitted for user {} ({})", id, command.userId(), command.type());
        return id;
    }
}
