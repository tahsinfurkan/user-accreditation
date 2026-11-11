package com.willowwealth.accreditation.adapter.in.response;

import java.util.List;
import java.util.UUID;

public record AccreditationHistoryResponse(UUID accreditationId, String userId, List<StatusChangeResponse> history) {
}
