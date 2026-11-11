package com.willowwealth.accreditation.adapter.out.persistence.repository;

import com.willowwealth.accreditation.adapter.out.persistence.entity.DocumentEntity;
import com.willowwealth.accreditation.adapter.out.persistence.mapper.DocumentEntityMapper;
import com.willowwealth.accreditation.application.port.out.DocumentRepository;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.UUID;

import static com.willowwealth.accreditation.config.CircuitBreakerProducer.DATABASE;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository, PanacheRepositoryBase<DocumentEntity, UUID> {

    private final DocumentEntityMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    @ApplyGuard(DATABASE)
    public DocumentReference save(Document document) {
        var reference = DocumentReference.of(UUID.randomUUID(), document);
        persist(mapper.toEntity(reference, document.content(), clock.instant()));
        log.debug("Stored document {} ({} bytes, sha256 {})", reference.id(), reference.sizeBytes(), reference.sha256());
        return reference;
    }
}
