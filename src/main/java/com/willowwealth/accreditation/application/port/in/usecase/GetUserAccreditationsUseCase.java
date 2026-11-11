package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.query.GetUserAccreditationsQuery;
import com.willowwealth.accreditation.domain.exception.UserNotFoundException;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;

import java.util.List;

public interface GetUserAccreditationsUseCase {

    /**
     * Returns the user's accreditations, oldest first.
     *
     * @throws UserNotFoundException if the user has no accreditations at all
     */
    List<AccreditationSummary> get(GetUserAccreditationsQuery query);
}
