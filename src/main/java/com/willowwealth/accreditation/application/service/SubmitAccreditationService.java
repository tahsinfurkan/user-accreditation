package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.command.SubmitAccreditationCommand;
import com.willowwealth.accreditation.application.port.in.usecase.SubmitAccreditationUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.application.port.out.DocumentRepository;
import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.exception.PendingAccreditationExistsException;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.ZonedDateTime;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubmitAccreditationService implements SubmitAccreditationUseCase {

    private final AccreditationRepository accreditationRepository;
    private final DocumentRepository documentRepository;
    private final AccreditationEventPublisher eventPublisher;
    private final Clock clock;

    // No transaction here on purpose: the document repository commits on its own,
    // so no database connection is held while waiting for Kafka.
    @Override
    public AccreditationId submit(SubmitAccreditationCommand command) {
        // Checked before the document is stored, so a rejected request leaves nothing behind.
        if (accreditationRepository.existsPendingFor(command.userId())) {
            throw new PendingAccreditationExistsException(command.userId());
        }
        var id = AccreditationId.newId();
        var document = documentRepository.save(command.document());

        eventPublisher.publish(Accreditation.submit(id, command.userId(), command.type(), document, ChangeOrigin.ADMIN,
                ZonedDateTime.now(clock)));
        log.info("Accreditation {} submitted for user {} ({})", id, command.userId(), command.type());
        return id;
    }
}
