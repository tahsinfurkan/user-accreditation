package com.willowwealth.accreditation.domain.event;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.UserId;

import java.time.Instant;
import java.util.UUID;

public sealed interface AccreditationEvent permits AccreditationSubmitted, AccreditationStatusChanged {

    UUID eventId();

    AccreditationId accreditationId();

    UserId userId();

    Instant occurredAt();
}
