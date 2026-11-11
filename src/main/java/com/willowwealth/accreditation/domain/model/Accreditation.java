package com.willowwealth.accreditation.domain.model;

import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder
public record Accreditation(
        AccreditationId id,
        UserId userId,
        AccreditationType type,
        AccreditationStatus status,
        UUID documentId,
        Instant createdAt,
        Instant updatedAt) {

    public Accreditation {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(userId, "user id is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(status, "status is required");
    }

    public static AccreditationSubmitted submit(AccreditationId id, UserId userId, AccreditationType type,
                                                DocumentReference document, Instant now) {
        return new AccreditationSubmitted(UUID.randomUUID(), id, userId, type, document, now);
    }

    public static Accreditation from(AccreditationSubmitted event) {
        return Accreditation.builder()
                .id(event.accreditationId())
                .userId(event.userId())
                .type(event.type())
                .status(AccreditationStatus.PENDING)
                .documentId(event.document().id())
                .createdAt(event.occurredAt())
                .updatedAt(event.occurredAt())
                .build();
    }
}
