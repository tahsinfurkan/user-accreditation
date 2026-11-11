package com.willowwealth.accreditation.adapter.in.mapper;

import com.willowwealth.accreditation.adapter.in.request.DocumentPayload;
import com.willowwealth.accreditation.adapter.in.request.FinalizeAccreditationRequest;
import com.willowwealth.accreditation.adapter.in.request.SubmitAccreditationRequest;
import com.willowwealth.accreditation.application.port.in.command.FinalizeAccreditationCommand;
import com.willowwealth.accreditation.application.port.in.command.SubmitAccreditationCommand;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;
import java.util.UUID;

@Mapper
public interface AccreditationRequestMapper {

    @Mapping(target = "type", source = "accreditationType")
    SubmitAccreditationCommand toCommand(SubmitAccreditationRequest request);

    @Mapping(target = "accreditationId", source = "accreditationId")
    @Mapping(target = "outcome", source = "request.outcome")
    FinalizeAccreditationCommand toCommand(UUID accreditationId, FinalizeAccreditationRequest request);

    Document toDocument(DocumentPayload payload);

    default AccreditationId toAccreditationId(UUID value) {
        return new AccreditationId(value);
    }

    default UserId toUserId(String value) {
        return new UserId(value);
    }

    default byte[] decodeBase64(String content) {
        return Base64.getDecoder().decode(content);
    }
}
