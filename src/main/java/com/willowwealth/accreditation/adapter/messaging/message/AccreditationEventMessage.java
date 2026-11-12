package com.willowwealth.accreditation.adapter.messaging.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;

import java.time.ZonedDateTime;
import java.util.UUID;

// Wire format of the accreditation topic. Shared by the publisher and the consumer.
// Unknown fields are ignored so that new fields can be added without breaking readers.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccreditationEventMessage(
        UUID eventId,
        EventType eventType,
        // Missing on the events written before it existed, which are version 1.
        Integer schemaVersion,
        UUID accreditationId,
        String userId,
        AccreditationType accreditationType,
        AccreditationStatus previousStatus,
        AccreditationStatus newStatus,
        ChangeOrigin origin,
        ZonedDateTime occurredAt,
        DocumentMessage document) {

    // Goes up only with a change that consumers reading an older version can't handle.
    public static final int SCHEMA_VERSION = 1;

    public enum EventType {
        ACCREDITATION_SUBMITTED,
        ACCREDITATION_STATUS_CHANGED
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentMessage(UUID documentId, String name, String mimeType, long sizeBytes, String sha256) {
    }
}
