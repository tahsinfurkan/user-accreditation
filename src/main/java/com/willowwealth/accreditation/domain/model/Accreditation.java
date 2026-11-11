package com.willowwealth.accreditation.domain.model;

import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder
@With
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
                                                DocumentReference document, ChangeOrigin origin, Instant now) {
        return new AccreditationSubmitted(UUID.randomUUID(), id, userId, type, document, origin, now);
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

    public AccreditationStatusChanged changeStatus(AccreditationStatus target, ChangeOrigin origin, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
        return new AccreditationStatusChanged(UUID.randomUUID(), id, userId, type, status, target, origin, now);
    }

    // An event is decided against one specific status. If the status has moved on since,
    // applying it anyway would bypass the transition rules, so it is rejected as well.
    public Accreditation apply(AccreditationStatusChanged event) {
        if (event.previousStatus() != status || !status.canTransitionTo(event.newStatus())) {
            throw new InvalidStatusTransitionException(id, status, event.newStatus());
        }
        return withStatus(event.newStatus()).withUpdatedAt(event.occurredAt());
    }
}
