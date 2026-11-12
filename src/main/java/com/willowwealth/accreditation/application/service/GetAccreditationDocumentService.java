package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.query.GetAccreditationDocumentQuery;
import com.willowwealth.accreditation.application.port.in.usecase.GetAccreditationDocumentUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.application.port.out.DocumentRepository;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.model.Document;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class GetAccreditationDocumentService implements GetAccreditationDocumentUseCase {

    private final AccreditationRepository accreditations;
    private final DocumentRepository documents;

    @Override
    public Document get(GetAccreditationDocumentQuery query) {
        var accreditation = accreditations.findById(query.accreditationId())
                .orElseThrow(() -> new AccreditationNotFoundException(query.accreditationId()));
        // The document is stored before the submission is published, so an accreditation always has one.
        var document = documents.findDocument(accreditation.documentId())
                .orElseThrow(() -> new IllegalStateException("Document %s of accreditation %s is missing"
                        .formatted(accreditation.documentId(), accreditation.id())));
        log.debug("Serving document {} of accreditation {}", accreditation.documentId(), accreditation.id());
        return document;
    }
}
