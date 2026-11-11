package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryAccreditationRepository implements AccreditationRepository {

    final Map<AccreditationId, Accreditation> byId = new HashMap<>();
    int inserts;

    @Override
    public boolean exists(AccreditationId id) {
        return byId.containsKey(id);
    }

    @Override
    public Optional<Accreditation> findById(AccreditationId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void insert(Accreditation accreditation) {
        byId.put(accreditation.id(), accreditation);
        inserts++;
    }

    @Override
    public void update(Accreditation accreditation) {
        byId.put(accreditation.id(), accreditation);
    }

    @Override
    public boolean existsPendingFor(UserId userId) {
        return byId.values().stream()
                .anyMatch(accreditation -> accreditation.userId().equals(userId)
                        && accreditation.status() == AccreditationStatus.PENDING);
    }

    @Override
    public List<AccreditationSummary> findSummariesByUserId(UserId userId) {
        return byId.values().stream()
                .filter(accreditation -> accreditation.userId().equals(userId))
                .sorted(Comparator.comparing(Accreditation::createdAt))
                .map(accreditation -> new AccreditationSummary(accreditation.id(), accreditation.type(),
                        accreditation.status()))
                .toList();
    }
}
