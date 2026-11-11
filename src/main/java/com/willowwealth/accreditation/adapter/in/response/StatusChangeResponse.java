package com.willowwealth.accreditation.adapter.in.response;

import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record StatusChangeResponse(
        @Schema(examples = {"0b6f2c1e-3d4a-4f5b-8c7d-9e0f1a2b3c4d", "5f1c2a8e-6d1b-4c47-9a0e-2b8f3c9d7e10"})
        UUID eventId,

        @Schema(description = "Empty for the submission; only PENDING and CONFIRMED can be changed",
                examples = {"PENDING", "CONFIRMED"})
        AccreditationStatus previousStatus,

        @Schema(examples = {"PENDING", "CONFIRMED", "EXPIRED", "FAILED"})
        AccreditationStatus newStatus,

        @Schema(description = "ADMIN for changes made through the API, SYSTEM_EXPIRY for the 30-day expiry",
                examples = {"ADMIN", "SYSTEM_EXPIRY"})
        ChangeOrigin origin,

        @Schema(examples = {"2026-10-01T09:30:00Z", "2026-11-01T15:00:00Z"})
        Instant occurredAt) {
}
