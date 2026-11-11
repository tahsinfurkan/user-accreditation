package com.willowwealth.accreditation.adapter.out.persistence.mapper;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.projection.AccreditationSummaryRow;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AccreditationEntityMapper {

    @Mapping(target = "version", ignore = true)
    AccreditationEntity toEntity(Accreditation accreditation);

    List<AccreditationSummary> toSummaries(List<AccreditationSummaryRow> rows);

    default UUID fromAccreditationId(AccreditationId id) {
        return id.value();
    }

    default AccreditationId toAccreditationId(UUID id) {
        return new AccreditationId(id);
    }

    default String fromUserId(UserId userId) {
        return userId.value();
    }
}
