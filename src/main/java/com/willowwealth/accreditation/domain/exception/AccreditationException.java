package com.willowwealth.accreditation.domain.exception;

// Messages are meant for the logs. What the client sees comes from the API's own catalog.
public abstract sealed class AccreditationException extends RuntimeException
        permits AccreditationNotFoundException, UserNotFoundException,
        PendingAccreditationExistsException, InvalidStatusTransitionException {

    protected AccreditationException(String message) {
        super(message);
    }
}
