package com.willowwealth.accreditation.adapter.messaging.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;

import java.time.Instant;
import java.util.UUID;

// Wire format of the accreditation topic. Shared by the publisher and the consumer.
// Unknown fields are ignored so that new fields can be added without breaking readers.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccreditationEventMessage(
        UUID eventId,
        EventType eventType,
        UUID accreditationId,
        String userId,
        AccreditationType accreditationType,
        AccreditationStatus newStatus,
        Instant occurredAt,
        DocumentMessage document) {

    public enum EventType {
        ACCREDITATION_SUBMITTED
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentMessage(UUID documentId, String name, String mimeType, long sizeBytes, String sha256) {
    }
}
