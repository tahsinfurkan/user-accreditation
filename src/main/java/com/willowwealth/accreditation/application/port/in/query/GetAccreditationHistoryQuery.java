package com.willowwealth.accreditation.application.port.in.query;

import com.willowwealth.accreditation.domain.model.AccreditationId;

public record GetAccreditationHistoryQuery(AccreditationId accreditationId) {
}
