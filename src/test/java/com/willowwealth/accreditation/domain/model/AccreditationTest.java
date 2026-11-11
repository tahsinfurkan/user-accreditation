package com.willowwealth.accreditation.domain.model;

import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.ADMIN;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.FAILED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccreditationTest {

    private static final UserId USER = new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-01T10:00:00Z");
    private static final DocumentReference DOCUMENT =
            new DocumentReference(UUID.randomUUID(), "2018.pdf", "application/pdf", 42, "c0ffee");

    @Test
    void newSubmissionStartsAsPending() {
        var id = AccreditationId.newId();

        AccreditationSubmitted event = Accreditation.submit(id, USER, BY_INCOME, DOCUMENT, SUBMITTED_AT);
        Accreditation accreditation = Accreditation.from(event);

        assertThat(event.eventId()).isNotNull();
        assertThat(accreditation.id()).isEqualTo(id);
        assertThat(accreditation.status()).isEqualTo(PENDING);
        assertThat(accreditation.documentId()).isEqualTo(DOCUMENT.id());
        assertThat(accreditation.createdAt()).isEqualTo(SUBMITTED_AT);
        assertThat(accreditation.updatedAt()).isEqualTo(SUBMITTED_AT);
    }

    @Test
    void confirmingPendingAccreditationRecordsTheChange() {
        var confirmedAt = SUBMITTED_AT.plus(Duration.ofDays(1));

        AccreditationStatusChanged event = pending().changeStatus(CONFIRMED, ADMIN, confirmedAt);

        assertThat(event.previousStatus()).isEqualTo(PENDING);
        assertThat(event.newStatus()).isEqualTo(CONFIRMED);
        assertThat(event.origin()).isEqualTo(ADMIN);
        assertThat(event.occurredAt()).isEqualTo(confirmedAt);
    }

    @Test
    void failedAccreditationCannotBeUpdated() {
        var failed = pending().withStatus(FAILED);

        assertThatThrownBy(() -> failed.changeStatus(CONFIRMED, ADMIN, SUBMITTED_AT))
                .isInstanceOfSatisfying(InvalidStatusTransitionException.class, e -> {
                    assertThat(e.getCurrentStatus()).isEqualTo(FAILED);
                    assertThat(e.getRequestedStatus()).isEqualTo(CONFIRMED);
                });
    }

    @Test
    void applyingStatusChangeMovesStatusAndUpdatedAt() {
        var accreditation = pending();
        var confirmedAt = SUBMITTED_AT.plus(Duration.ofHours(3));

        var confirmed = accreditation.apply(accreditation.changeStatus(CONFIRMED, ADMIN, confirmedAt));

        assertThat(confirmed.status()).isEqualTo(CONFIRMED);
        assertThat(confirmed.updatedAt()).isEqualTo(confirmedAt);
        assertThat(confirmed.createdAt()).isEqualTo(SUBMITTED_AT);
    }

    @Test
    void rejectsChangeDecidedAgainstADifferentStatus() {
        var confirmed = pending().withStatus(CONFIRMED);
        // CONFIRMED -> EXPIRED is allowed, but this event was decided while the status was PENDING.
        var event = new AccreditationStatusChanged(UUID.randomUUID(), confirmed.id(), USER,
                BY_INCOME, PENDING, EXPIRED, ADMIN, SUBMITTED_AT);

        assertThatThrownBy(() -> confirmed.apply(event))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    private static Accreditation pending() {
        return Accreditation.from(Accreditation.submit(AccreditationId.newId(), USER, BY_INCOME, DOCUMENT, SUBMITTED_AT));
    }
}
