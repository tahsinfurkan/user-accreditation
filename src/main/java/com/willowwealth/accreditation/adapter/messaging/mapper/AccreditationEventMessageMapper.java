package com.willowwealth.accreditation.adapter.messaging.mapper;

import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage.DocumentMessage;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.UserId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(imports = AccreditationEventMessage.class)
public interface AccreditationEventMessageMapper {

    default AccreditationEventMessage toMessage(AccreditationEvent event) {
        return switch (event) {
            case AccreditationSubmitted submitted -> fromSubmitted(submitted);
            case AccreditationStatusChanged change -> fromStatusChanged(change);
        };
    }

    default AccreditationEvent toEvent(AccreditationEventMessage message) {
        if (message.schemaVersion() != null && message.schemaVersion() > AccreditationEventMessage.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Event %s has schema version %d, this consumer reads up to %d"
                    .formatted(message.eventId(), message.schemaVersion(), AccreditationEventMessage.SCHEMA_VERSION));
        }
        return switch (message.eventType()) {
            case ACCREDITATION_SUBMITTED -> toSubmitted(message);
            case ACCREDITATION_STATUS_CHANGED -> toStatusChanged(message);
        };
    }

    @Mapping(target = "eventType", constant = "ACCREDITATION_SUBMITTED")
    @Mapping(target = "schemaVersion", expression = "java(AccreditationEventMessage.SCHEMA_VERSION)")
    @Mapping(target = "accreditationType", source = "type")
    @Mapping(target = "previousStatus", ignore = true)
    @Mapping(target = "newStatus", constant = "PENDING")
    AccreditationEventMessage fromSubmitted(AccreditationSubmitted event);

    @Mapping(target = "eventType", constant = "ACCREDITATION_STATUS_CHANGED")
    @Mapping(target = "schemaVersion", expression = "java(AccreditationEventMessage.SCHEMA_VERSION)")
    @Mapping(target = "accreditationType", source = "type")
    @Mapping(target = "document", ignore = true)
    AccreditationEventMessage fromStatusChanged(AccreditationStatusChanged event);

    @Mapping(target = "type", source = "accreditationType")
    AccreditationSubmitted toSubmitted(AccreditationEventMessage message);

    @Mapping(target = "type", source = "accreditationType")
    AccreditationStatusChanged toStatusChanged(AccreditationEventMessage message);

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
