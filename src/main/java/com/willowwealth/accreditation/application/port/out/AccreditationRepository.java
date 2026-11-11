package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccreditationRepository {

    Optional<Accreditation> findById(AccreditationId id);

    void insert(Accreditation accreditation);

    void update(Accreditation accreditation);

    boolean existsPendingFor(UserId userId);

    /**
     * Returns up to {@code limit} CONFIRMED accreditations last updated before {@code cutoff}, ordered by update time
     * and then id. For the next page pass the last accreditation of the previous one as {@code after}; for the first
     * page pass {@code null}.
     */
    List<Accreditation> findConfirmedUpdatedBefore(Instant cutoff, Accreditation after, int limit);

    /**
     * Returns the user's accreditations, oldest first.
     */
    List<AccreditationSummary> findSummariesByUserId(UserId userId);
}
