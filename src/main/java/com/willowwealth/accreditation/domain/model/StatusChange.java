package com.willowwealth.accreditation.domain.model;

import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.event.ChangeOrigin;

import java.time.Instant;
import java.util.UUID;

// One entry of an accreditation's history. The submission is the first entry and has no previous status.
public record StatusChange(
        UUID eventId,
        AccreditationId accreditationId,
        AccreditationStatus previousStatus,
        AccreditationStatus newStatus,
        ChangeOrigin origin,
        Instant occurredAt) {

    public static StatusChange from(AccreditationEvent event) {
        return switch (event) {
            case AccreditationSubmitted submitted -> new StatusChange(submitted.eventId(), submitted.accreditationId(),
                    null, AccreditationStatus.PENDING, submitted.origin(), submitted.occurredAt());
            case AccreditationStatusChanged change -> new StatusChange(change.eventId(), change.accreditationId(),
                    change.previousStatus(), change.newStatus(), change.origin(), change.occurredAt());
        };
    }
}
