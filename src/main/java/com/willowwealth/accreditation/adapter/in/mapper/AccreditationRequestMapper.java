package com.willowwealth.accreditation.adapter.in.mapper;

import com.willowwealth.accreditation.adapter.in.request.DocumentPayload;
import com.willowwealth.accreditation.adapter.in.request.SubmitAccreditationRequest;
import com.willowwealth.accreditation.application.port.in.command.SubmitAccreditationCommand;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Base64;

@Mapper
public interface AccreditationRequestMapper {

    @Mapping(target = "type", source = "accreditationType")
    SubmitAccreditationCommand toCommand(SubmitAccreditationRequest request);

    Document toDocument(DocumentPayload payload);

    default UserId toUserId(String value) {
        return new UserId(value);
    }

    default byte[] decodeBase64(String content) {
        return Base64.getDecoder().decode(content);
    }
}
