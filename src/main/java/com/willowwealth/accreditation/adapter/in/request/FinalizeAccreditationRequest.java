package com.willowwealth.accreditation.adapter.in.request;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record FinalizeAccreditationRequest(
        @NotNull
        @Schema(examples = {"CONFIRMED", "EXPIRED", "FAILED"})
        AccreditationOutcome outcome) {
}
