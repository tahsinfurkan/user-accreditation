package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.out.AccreditationHistoryRepository;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.StatusChange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

class InMemoryAccreditationHistoryRepository implements AccreditationHistoryRepository {

    final List<StatusChange> changes = new ArrayList<>();

    @Override
    public boolean contains(UUID eventId) {
        return changes.stream().anyMatch(change -> change.eventId().equals(eventId));
    }

    @Override
    public void append(StatusChange change) {
        changes.add(change);
    }

    @Override
    public List<StatusChange> findByAccreditationId(AccreditationId accreditationId) {
        return changes.stream()
                .filter(change -> change.accreditationId().equals(accreditationId))
                .sorted(Comparator.comparing(StatusChange::occurredAt))
                .toList();
    }
}
