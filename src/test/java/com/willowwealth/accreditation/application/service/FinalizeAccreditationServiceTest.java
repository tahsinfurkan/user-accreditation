package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.command.FinalizeAccreditationCommand;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.ADMIN;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinalizeAccreditationServiceTest {

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-11-05T09:00:00Z");

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final List<AccreditationEvent> published = new ArrayList<>();
    private final FinalizeAccreditationService service = new FinalizeAccreditationService(
            accreditations, published::add, Clock.fixed(NOW.toInstant(), NOW.getZone()));

    @Test
    void publishesTheStatusChange() {
        var accreditation = pendingAccreditation();
        accreditations.insert(accreditation);

        service.finalizeAccreditation(new FinalizeAccreditationCommand(accreditation.id(), CONFIRMED));

        assertThat(published).singleElement().isInstanceOfSatisfying(AccreditationStatusChanged.class, change -> {
            assertThat(change.accreditationId()).isEqualTo(accreditation.id());
            assertThat(change.previousStatus()).isEqualTo(PENDING);
            assertThat(change.newStatus()).isEqualTo(CONFIRMED);
            assertThat(change.origin()).isEqualTo(ADMIN);
            assertThat(change.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void reportsAnUnknownAccreditation() {
        var command = new FinalizeAccreditationCommand(AccreditationId.newId(), CONFIRMED);

        assertThatThrownBy(() -> service.finalizeAccreditation(command))
                .isInstanceOf(AccreditationNotFoundException.class);
        assertThat(published).isEmpty();
    }

    @Test
    void publishesNothingWhenTheStatusDoesNotAllowTheOutcome() {
        var accreditation = pendingAccreditation();
        accreditations.insert(accreditation);
        var command = new FinalizeAccreditationCommand(accreditation.id(), EXPIRED);

        assertThatThrownBy(() -> service.finalizeAccreditation(command))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(published).isEmpty();
    }

    @Test
    void publishesNothingWhenTheAccreditationAlreadyHasTheStatus() {
        var accreditation = pendingAccreditation().withStatus(CONFIRMED);
        accreditations.insert(accreditation);

        service.finalizeAccreditation(new FinalizeAccreditationCommand(accreditation.id(), CONFIRMED));

        assertThat(published).isEmpty();
    }

    private static Accreditation pendingAccreditation() {
        var submittedAt = NOW.minusSeconds(3600);
        return Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V"))
                .type(BY_INCOME)
                .status(PENDING)
                .documentId(UUID.randomUUID())
                .createdAt(submittedAt)
                .updatedAt(submittedAt)
                .build();
    }
}
