package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;

import java.util.List;

public interface AccreditationRepository {

    boolean exists(AccreditationId id);

    void insert(Accreditation accreditation);

    boolean existsPendingFor(UserId userId);

    /**
     * Returns the user's accreditations, oldest first.
     */
    List<AccreditationSummary> findSummariesByUserId(UserId userId);
}
