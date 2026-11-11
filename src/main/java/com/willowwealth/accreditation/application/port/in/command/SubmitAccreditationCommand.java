package com.willowwealth.accreditation.application.port.in.command;

import com.willowwealth.accreditation.domain.model.AccreditationType;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.UserId;

public record SubmitAccreditationCommand(UserId userId, AccreditationType type, Document document) {
}
