package com.willowwealth.accreditation.adapter.in.request;

// The statuses an admin may set. PENDING is left out, so asking for it fails like any unknown value.
public enum AccreditationOutcome {
    CONFIRMED,
    EXPIRED,
    FAILED
}
