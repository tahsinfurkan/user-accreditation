package com.willowwealth.accreditation.adapter.in.mapper;

import com.willowwealth.accreditation.adapter.in.response.AccreditationStatusResponse;
import com.willowwealth.accreditation.adapter.in.response.UserAccreditationsResponse;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AccreditationResponseMapper {

    @Mapping(target = "accreditationType", source = "type")
    AccreditationStatusResponse toStatusResponse(AccreditationSummary accreditation);

    // The spec keys the statuses by accreditation id; the map keeps them in the order they were created.
    default UserAccreditationsResponse toResponse(UserId userId, List<AccreditationSummary> accreditations) {
        var statuses = new LinkedHashMap<UUID, AccreditationStatusResponse>();
        accreditations.forEach(accreditation -> statuses.put(accreditation.id().value(), toStatusResponse(accreditation)));
        return new UserAccreditationsResponse(userId.value(), statuses);
    }
}
