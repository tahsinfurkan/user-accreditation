package com.willowwealth.accreditation.domain.exception;

import com.willowwealth.accreditation.domain.model.UserId;
import lombok.Getter;

@Getter
public final class UserNotFoundException extends AccreditationException {

    private final UserId userId;

    public UserNotFoundException(UserId userId) {
        super("No accreditations found for user " + userId);
        this.userId = userId;
    }
}
