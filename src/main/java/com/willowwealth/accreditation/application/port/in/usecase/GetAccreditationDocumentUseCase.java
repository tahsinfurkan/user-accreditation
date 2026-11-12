package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.query.GetAccreditationDocumentQuery;
import com.willowwealth.accreditation.domain.model.Document;

public interface GetAccreditationDocumentUseCase {

    /**
     * Returns the document the accreditation was submitted with.
     *
     * @throws com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException if there is no such
     *                                                                                        accreditation
     */
    Document get(GetAccreditationDocumentQuery query);
}
