package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.query.GetAccreditationHistoryQuery;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.model.AccreditationHistory;

public interface GetAccreditationHistoryUseCase {

    /**
     * Returns every applied change of the accreditation, oldest first, starting with its submission.
     *
     * @throws AccreditationNotFoundException if there is no such accreditation
     */
    AccreditationHistory get(GetAccreditationHistoryQuery query);
}
