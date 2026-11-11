package com.willowwealth.accreditation.domain.model;

public enum AccreditationStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    FAILED;

    // The spec only asks for CONFIRMED accreditations to expire (via the API or the
    // 30-day job). Expiring stale PENDING submissions after a few days would make
    // sense too, but it's left out on purpose to stick to the spec.
    public boolean canTransitionTo(AccreditationStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == FAILED;
            case CONFIRMED -> target == EXPIRED;
            case EXPIRED, FAILED -> false;
        };
    }
}
