package com.willowwealth.accreditation.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccreditationId(UUID value) {

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
