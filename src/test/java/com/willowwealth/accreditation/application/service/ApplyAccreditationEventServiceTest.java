package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.domain.event.AccreditationSubmitted;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_NET_WORTH;
import static org.assertj.core.api.Assertions.assertThat;

class ApplyAccreditationEventServiceTest {

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final ApplyAccreditationEventService service = new ApplyAccreditationEventService(accreditations);

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
    void ignoresTheSameSubmissionDeliveredTwice() {
        var event = submitted();

        service.apply(event);
        service.apply(event);

        assertThat(accreditations.inserts).isEqualTo(1);
    }

    private static AccreditationSubmitted submitted() {
        var document = new DocumentReference(UUID.randomUUID(), "2018.pdf", "application/pdf", 42, "c0ffee");
        return Accreditation.submit(AccreditationId.newId(), new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V"),
                BY_NET_WORTH, document, Instant.parse("2026-11-01T10:00:00Z"));
    }
}
