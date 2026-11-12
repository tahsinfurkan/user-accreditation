package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.result.ProjectionResult;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;

public interface ApplyAccreditationEventUseCase {

    ProjectionResult apply(AccreditationEvent event);
}
