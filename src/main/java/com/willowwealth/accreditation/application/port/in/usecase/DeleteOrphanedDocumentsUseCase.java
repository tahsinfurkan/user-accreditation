package com.willowwealth.accreditation.application.port.in.usecase;

import com.willowwealth.accreditation.application.port.in.command.DeleteOrphanedDocumentsCommand;

public interface DeleteOrphanedDocumentsUseCase {

    /**
     * Deletes the documents no accreditation refers to that are older than the grace period.
     *
     * @return how many documents were deleted
     */
    int delete(DeleteOrphanedDocumentsCommand command);
}
