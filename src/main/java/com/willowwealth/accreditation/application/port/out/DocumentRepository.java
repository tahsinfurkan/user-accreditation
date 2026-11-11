package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.DocumentReference;

public interface DocumentRepository {

    DocumentReference save(Document document);
}
