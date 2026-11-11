package com.willowwealth.accreditation.adapter.in.request;

import com.willowwealth.accreditation.domain.model.AccreditationType;
import com.willowwealth.accreditation.domain.model.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SubmitAccreditationRequest(
        @NotNull
        @Pattern(regexp = UserId.PATTERN, message = "must be exactly 32 letters or digits")
        @Schema(examples = {"g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V", "Q3vT9cLw2KxM8nRb5HdZ1pYf7GsJ4uAe"})
        String userId,

        @NotNull
        @Schema(examples = {"BY_INCOME", "BY_NET_WORTH"})
        AccreditationType accreditationType,

        @NotNull
        @Valid
        DocumentPayload document) {
}
