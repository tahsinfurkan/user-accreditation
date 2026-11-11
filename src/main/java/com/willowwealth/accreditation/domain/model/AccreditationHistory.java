package com.willowwealth.accreditation.domain.model;

import java.util.List;

public record AccreditationHistory(AccreditationId accreditationId, UserId userId, List<StatusChange> changes) {
}
