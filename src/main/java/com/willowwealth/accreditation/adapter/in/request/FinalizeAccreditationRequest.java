package com.willowwealth.accreditation.adapter.in.request;

import jakarta.validation.constraints.NotNull;

public record FinalizeAccreditationRequest(@NotNull AccreditationOutcome outcome) {
}
