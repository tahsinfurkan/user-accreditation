package com.willowwealth.accreditation.adapter.in.request;

import com.willowwealth.accreditation.domain.model.AccreditationType;
import com.willowwealth.accreditation.domain.model.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubmitAccreditationRequest(
        @NotNull
        @Pattern(regexp = UserId.PATTERN, message = "must be exactly 32 letters or digits")
        String userId,

        @NotNull
        AccreditationType accreditationType,

        @NotNull
        @Valid
        DocumentPayload document) {
}
