package com.willowwealth.accreditation.domain.model;

// What a client needs to know about an accreditation: which one it is, its type and where it stands.
public record AccreditationSummary(AccreditationId id, AccreditationType type, AccreditationStatus status) {
}
