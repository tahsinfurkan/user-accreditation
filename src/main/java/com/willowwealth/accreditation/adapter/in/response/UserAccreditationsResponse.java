package com.willowwealth.accreditation.adapter.in.response;

import java.util.SequencedMap;
import java.util.UUID;

public record UserAccreditationsResponse(String userId,
                                         SequencedMap<UUID, AccreditationStatusResponse> accreditationStatuses) {
}
