package com.willowwealth.accreditation.domain.exception;

import com.willowwealth.accreditation.domain.model.UserId;
import lombok.Getter;

@Getter
public final class PendingAccreditationExistsException extends AccreditationException {

    private final UserId userId;

    public PendingAccreditationExistsException(UserId userId) {
        super("User " + userId + " already has a pending accreditation");
        this.userId = userId;
    }
}
