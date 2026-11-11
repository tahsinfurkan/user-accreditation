package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;

public interface ApplyAccreditationEventUseCase {

    void apply(AccreditationSubmitted event);
}
