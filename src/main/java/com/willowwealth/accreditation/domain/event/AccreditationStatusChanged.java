package com.willowwealth.accreditation.domain.event;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;
import com.willowwealth.accreditation.domain.model.UserId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record AccreditationStatusChanged(
        UUID eventId,
        AccreditationId accreditationId,
        UserId userId,
        AccreditationType type,
        AccreditationStatus previousStatus,
        AccreditationStatus newStatus,
        ChangeOrigin origin,
        ZonedDateTime occurredAt) implements AccreditationEvent {
}
