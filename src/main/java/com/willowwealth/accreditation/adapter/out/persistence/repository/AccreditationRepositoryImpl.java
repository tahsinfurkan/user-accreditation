package com.willowwealth.accreditation.adapter.out.persistence.repository;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.mapper.AccreditationEntityMapper;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class AccreditationRepositoryImpl
        implements AccreditationRepository, PanacheRepositoryBase<AccreditationEntity, UUID> {

    private final AccreditationEntityMapper mapper;

    @Override
    public boolean exists(AccreditationId id) {
        return count("id", id.value()) > 0;
    }

    @Override
    public void insert(Accreditation accreditation) {
        persist(mapper.toEntity(accreditation));
    }
}
