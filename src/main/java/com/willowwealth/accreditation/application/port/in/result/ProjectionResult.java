package com.willowwealth.accreditation.application.port.in.result;

public enum ProjectionResult {
    APPLIED,
    DUPLICATE,
    // Rejections: the event doesn't fit what is stored, usually because another event got there first. They are not
    // errors, and retrying the event wouldn't change them.
    PENDING_EXISTS,
    UNKNOWN_ACCREDITATION,
    STATUS_CHANGED
}
