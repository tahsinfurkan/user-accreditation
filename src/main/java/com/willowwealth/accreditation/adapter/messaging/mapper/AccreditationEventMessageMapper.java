package com.willowwealth.accreditation.adapter.messaging.mapper;

import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage.DocumentMessage;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper
public interface AccreditationEventMessageMapper {

    @Mapping(target = "eventType", constant = "ACCREDITATION_SUBMITTED")
    @Mapping(target = "accreditationType", source = "type")
    @Mapping(target = "newStatus", constant = "PENDING")
    AccreditationEventMessage toMessage(AccreditationSubmitted event);

    @Mapping(target = "type", source = "accreditationType")
    AccreditationSubmitted toSubmitted(AccreditationEventMessage message);

    @Mapping(target = "documentId", source = "id")
    DocumentMessage toDocumentMessage(DocumentReference document);

    @Mapping(target = "id", source = "documentId")
    DocumentReference toDocumentReference(DocumentMessage document);

    default UUID fromAccreditationId(AccreditationId id) {
        return id.value();
    }

    default AccreditationId toAccreditationId(UUID value) {
        return new AccreditationId(value);
    }

    default String fromUserId(UserId userId) {
        return userId.value();
    }

    default UserId toUserId(String value) {
        return new UserId(value);
    }
}
