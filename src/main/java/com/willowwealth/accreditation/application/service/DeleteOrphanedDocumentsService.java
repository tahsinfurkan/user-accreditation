package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.command.DeleteOrphanedDocumentsCommand;
import com.willowwealth.accreditation.application.port.in.usecase.DeleteOrphanedDocumentsUseCase;
import com.willowwealth.accreditation.application.port.out.DocumentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.ZonedDateTime;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteOrphanedDocumentsService implements DeleteOrphanedDocumentsUseCase {

    private final DocumentRepository documents;
    private final Clock clock;

    // A document is stored before its submission is published, and the consumer refers to it a moment later. The
    // ones left without an accreditation belong to submissions that failed to publish or that the consumer rejected.
    // The grace period keeps documents of events still waiting in Kafka.
    @Override
    @Transactional
    public int delete(DeleteOrphanedDocumentsCommand command) {
        return documents.deleteUnreferencedCreatedBefore(ZonedDateTime.now(clock).minus(command.gracePeriod()));
    }
}
