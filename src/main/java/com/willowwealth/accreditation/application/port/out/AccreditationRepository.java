package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;

public interface AccreditationRepository {

    boolean exists(AccreditationId id);

    void insert(Accreditation accreditation);
}
