package com.willowwealth.accreditation.adapter.in.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

public record AccreditationIdResponse(
        @Schema(examples = {"87bb6030-458e-11ed-b023-039b275a916a", "c031a5da-d59a-4d35-b1f0-0f8324dcc156"})
        UUID accreditationId) {
}
