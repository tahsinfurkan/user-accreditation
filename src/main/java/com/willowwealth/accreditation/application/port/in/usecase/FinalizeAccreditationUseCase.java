package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.command.FinalizeAccreditationCommand;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;

public interface FinalizeAccreditationUseCase {

    /**
     * Publishes the status change. The accreditation shows the new status once its event is consumed. Nothing is
     * published if the accreditation already has that status.
     *
     * @throws AccreditationNotFoundException   if there is no such accreditation
     * @throws InvalidStatusTransitionException if the current status does not allow the outcome
     */
    void finalizeAccreditation(FinalizeAccreditationCommand command);
}
