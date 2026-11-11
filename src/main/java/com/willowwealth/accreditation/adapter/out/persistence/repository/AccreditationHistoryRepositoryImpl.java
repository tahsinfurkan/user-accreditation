package com.willowwealth.accreditation.adapter.out.persistence.repository;

import com.willowwealth.accreditation.adapter.out.persistence.entity.StatusChangeEntity;
import com.willowwealth.accreditation.adapter.out.persistence.mapper.StatusChangeEntityMapper;
import com.willowwealth.accreditation.application.port.out.AccreditationHistoryRepository;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.StatusChange;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class AccreditationHistoryRepositoryImpl
        implements AccreditationHistoryRepository, PanacheRepositoryBase<StatusChangeEntity, UUID> {

    private final StatusChangeEntityMapper mapper;
    private final Clock clock;

    @Override
    public boolean contains(UUID eventId) {
        return count("eventId", eventId) > 0;
    }

    @Override
    public void append(StatusChange change) {
        persist(mapper.toEntity(change, clock.instant()));
    }

    @Override
    public List<StatusChange> findByAccreditationId(AccreditationId accreditationId) {
        return mapper.toDomain(list("accreditationId", Sort.by("occurredAt").and("recordedAt"), accreditationId.value()));
    }
}
