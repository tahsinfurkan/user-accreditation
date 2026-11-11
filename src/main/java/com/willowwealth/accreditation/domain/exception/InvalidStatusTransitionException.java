package com.willowwealth.accreditation.domain.exception;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import lombok.Getter;

@Getter
public final class InvalidStatusTransitionException extends AccreditationException {

    private final AccreditationId accreditationId;
    private final AccreditationStatus currentStatus;
    private final AccreditationStatus requestedStatus;

    public InvalidStatusTransitionException(AccreditationId accreditationId,
                                            AccreditationStatus currentStatus,
                                            AccreditationStatus requestedStatus) {
        super("Accreditation %s cannot move from %s to %s".formatted(accreditationId, currentStatus, requestedStatus));
        this.accreditationId = accreditationId;
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }
}
