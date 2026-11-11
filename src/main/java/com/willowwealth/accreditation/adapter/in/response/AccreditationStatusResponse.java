package com.willowwealth.accreditation.adapter.in.response;

import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record AccreditationStatusResponse(
        @Schema(examples = {"BY_INCOME", "BY_NET_WORTH"})
        AccreditationType accreditationType,

        @Schema(examples = {"PENDING", "CONFIRMED", "EXPIRED", "FAILED"})
        AccreditationStatus status) {
}
