package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.StatusChange;

import java.util.List;
import java.util.UUID;

public interface AccreditationHistoryRepository {

    boolean contains(UUID eventId);

    void append(StatusChange change);

    /**
     * Returns the accreditation's changes, oldest first.
     */
    List<StatusChange> findByAccreditationId(AccreditationId accreditationId);
}
