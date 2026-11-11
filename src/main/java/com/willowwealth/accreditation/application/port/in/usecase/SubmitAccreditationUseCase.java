package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.command.SubmitAccreditationCommand;
import com.willowwealth.accreditation.domain.model.AccreditationId;

public interface SubmitAccreditationUseCase {

    AccreditationId submit(SubmitAccreditationCommand command);
}
