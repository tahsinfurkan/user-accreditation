package com.willowwealth.accreditation.adapter.out.persistence.projection;

import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;

import java.util.UUID;

// Panache builds the select list from these component names, so they must match the entity's fields.
public record AccreditationSummaryRow(UUID id, AccreditationType type, AccreditationStatus status) {
}
