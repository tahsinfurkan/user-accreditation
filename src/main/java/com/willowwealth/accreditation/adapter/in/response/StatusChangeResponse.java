package com.willowwealth.accreditation.adapter.in.response;

import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;

import java.time.Instant;
import java.util.UUID;

public record StatusChangeResponse(UUID eventId, AccreditationStatus previousStatus, AccreditationStatus newStatus,
                                   ChangeOrigin origin, Instant occurredAt) {
}
