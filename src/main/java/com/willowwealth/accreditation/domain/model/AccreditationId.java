package com.willowwealth.accreditation.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccreditationId(UUID value) {

    // Public so that request validation can reuse the same rule.
    public static final String PATTERN =
            "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    public AccreditationId {
        Objects.requireNonNull(value, "accreditation id is required");
    }

    public static AccreditationId newId() {
        return new AccreditationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
