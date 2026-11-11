package com.willowwealth.accreditation.adapter.in.response;

import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;

public record AccreditationStatusResponse(AccreditationType accreditationType, AccreditationStatus status) {
}
