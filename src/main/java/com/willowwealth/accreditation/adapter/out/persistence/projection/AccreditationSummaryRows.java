package com.willowwealth.accreditation.adapter.out.persistence.projection;

import java.util.List;

// The Redis cache stores values as JSON. Wrapping the list keeps the element type, so rows come back as rows.
public record AccreditationSummaryRows(List<AccreditationSummaryRow> rows) {
}
