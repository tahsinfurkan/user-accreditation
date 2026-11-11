package com.willowwealth.accreditation.domain.exception;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import lombok.Getter;

@Getter
public final class AccreditationNotFoundException extends AccreditationException {

    private final AccreditationId accreditationId;

    public AccreditationNotFoundException(AccreditationId accreditationId) {
        super("Accreditation " + accreditationId + " does not exist");
        this.accreditationId = accreditationId;
    }
}
