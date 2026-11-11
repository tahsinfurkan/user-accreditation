package com.willowwealth.accreditation.adapter.out.persistence.mapper;

import com.willowwealth.accreditation.adapter.out.persistence.entity.StatusChangeEntity;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.StatusChange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface StatusChangeEntityMapper {

    @Mapping(target = "recordedAt", source = "recordedAt")
    StatusChangeEntity toEntity(StatusChange change, ZonedDateTime recordedAt);

    List<StatusChange> toDomain(List<StatusChangeEntity> entities);

    default UUID fromAccreditationId(AccreditationId id) {
        return id.value();
    }

    default AccreditationId toAccreditationId(UUID id) {
        return new AccreditationId(id);
    }
}
