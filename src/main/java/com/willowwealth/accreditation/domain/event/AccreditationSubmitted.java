package com.willowwealth.accreditation.domain.event;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationType;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.UserId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record AccreditationSubmitted(
        UUID eventId,
        AccreditationId accreditationId,
        UserId userId,
        AccreditationType type,
        DocumentReference document,
        ChangeOrigin origin,
        ZonedDateTime occurredAt) implements AccreditationEvent {
}
