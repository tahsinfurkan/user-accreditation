package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.domain.event.AccreditationEvent;

public interface ApplyAccreditationEventUseCase {

    void apply(AccreditationEvent event);
}
