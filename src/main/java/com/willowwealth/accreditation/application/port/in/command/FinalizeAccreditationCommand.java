package com.willowwealth.accreditation.application.port.in.command;

import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;

public record FinalizeAccreditationCommand(AccreditationId accreditationId, AccreditationStatus outcome) {
}
