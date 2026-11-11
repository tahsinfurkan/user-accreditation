package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.StatusChange;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.ADMIN;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.FAILED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_NET_WORTH;
import static org.assertj.core.api.Assertions.assertThat;

class ApplyAccreditationEventServiceTest {

    private static final UserId USER = new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-11-02T10:00:00Z");

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final InMemoryAccreditationHistoryRepository history = new InMemoryAccreditationHistoryRepository();
    private final ApplyAccreditationEventService service = new ApplyAccreditationEventService(accreditations, history);

    @Test
    void recordsSubmissionAsPending() {
        var event = submitted();

        service.apply(event);

        var accreditation = accreditations.byId.get(event.accreditationId());
        assertThat(accreditation.status()).isEqualTo(PENDING);
        assertThat(accreditation.type()).isEqualTo(BY_NET_WORTH);
        assertThat(accreditation.documentId()).isEqualTo(event.document().id());
        assertThat(accreditation.createdAt()).isEqualTo(event.occurredAt());
    }

    @Test
    void startsTheHistoryWithTheSubmission() {
        var event = submitted();

        service.apply(event);

        assertThat(history.changes).containsExactly(
                new StatusChange(event.eventId(), event.accreditationId(), null, PENDING, ADMIN, SUBMITTED_AT));
    }

    @Test
    void ignoresTheSameSubmissionDeliveredTwice() {
        var event = submitted();

        service.apply(event);
        service.apply(event);

        assertThat(accreditations.inserts).isEqualTo(1);
        assertThat(history.changes).hasSize(1);
    }

    @Test
    void rejectsASecondSubmissionWhileTheUserHasAPendingOne() {
        var first = submitted();
        var second = submitted();

        service.apply(first);
        service.apply(second);

        assertThat(accreditations.byId).containsOnlyKeys(first.accreditationId());
        assertThat(history.changes).extracting(StatusChange::eventId).containsExactly(first.eventId());
    }

    @Test
    void appliesAStatusChange() {
        var submission = submitted();
        service.apply(submission);
        var change = recorded(submission).changeStatus(CONFIRMED, ADMIN, DECIDED_AT);

        service.apply(change);

        var accreditation = recorded(submission);
        assertThat(accreditation.status()).isEqualTo(CONFIRMED);
        assertThat(accreditation.updatedAt()).isEqualTo(DECIDED_AT);
        assertThat(accreditation.createdAt()).isEqualTo(SUBMITTED_AT);
        assertThat(history.changes).extracting(StatusChange::eventId).containsExactly(submission.eventId(), change.eventId());
    }

    @Test
    void ignoresTheSameStatusChangeDeliveredTwice() {
        var submission = submitted();
        service.apply(submission);
        var change = recorded(submission).changeStatus(CONFIRMED, ADMIN, DECIDED_AT);

        service.apply(change);
        service.apply(change);

        assertThat(recorded(submission).status()).isEqualTo(CONFIRMED);
        assertThat(history.changes).extracting(StatusChange::eventId).containsExactly(submission.eventId(), change.eventId());
    }

    @Test
    void rejectsAChangeDecidedForAStatusThatHasMovedOn() {
        var submission = submitted();
        service.apply(submission);
        // Two admins decide on the same PENDING accreditation; the first change to arrive wins.
        var confirm = recorded(submission).changeStatus(CONFIRMED, ADMIN, DECIDED_AT);
        var fail = recorded(submission).changeStatus(FAILED, ADMIN, DECIDED_AT.plusSeconds(1));

        service.apply(confirm);
        service.apply(fail);

        assertThat(recorded(submission).status()).isEqualTo(CONFIRMED);
        assertThat(history.changes).extracting(StatusChange::eventId).containsExactly(submission.eventId(), confirm.eventId());
    }

    @Test
    void ignoresAChangeForAnUnknownAccreditation() {
        var change = new AccreditationStatusChanged(UUID.randomUUID(), AccreditationId.newId(), USER, BY_NET_WORTH,
                PENDING, CONFIRMED, ADMIN, DECIDED_AT);

        service.apply(change);

        assertThat(accreditations.byId).isEmpty();
        assertThat(history.changes).isEmpty();
    }

    private Accreditation recorded(AccreditationSubmitted submission) {
        return accreditations.byId.get(submission.accreditationId());
    }

    private static AccreditationSubmitted submitted() {
        var document = new DocumentReference(UUID.randomUUID(), "2018.pdf", "application/pdf", 42, "c0ffee");
        return Accreditation.submit(AccreditationId.newId(), USER, BY_NET_WORTH, document, ADMIN, SUBMITTED_AT);
    }
}
