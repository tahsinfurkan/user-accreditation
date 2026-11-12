package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.DocumentReference;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    DocumentReference save(Document document);

    Optional<Document> findDocument(UUID id);

    /**
     * Deletes the documents stored before {@code cutoff} that no accreditation refers to, and returns how many.
     */
    int deleteUnreferencedCreatedBefore(ZonedDateTime cutoff);
}
