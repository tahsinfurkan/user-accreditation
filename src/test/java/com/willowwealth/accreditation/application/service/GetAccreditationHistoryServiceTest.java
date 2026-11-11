package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.query.GetAccreditationHistoryQuery;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.StatusChange;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.ADMIN;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAccreditationHistoryServiceTest {

    private static final UserId USER = new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V");

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final InMemoryAccreditationHistoryRepository history = new InMemoryAccreditationHistoryRepository();
    private final GetAccreditationHistoryService service = new GetAccreditationHistoryService(accreditations, history);

    @Test
    void returnsTheChangesTogetherWithTheUser() {
        var document = new DocumentReference(UUID.randomUUID(), "2018.pdf", "application/pdf", 42, "c0ffee");
        var submission = Accreditation.submit(AccreditationId.newId(), USER, BY_INCOME, document, ADMIN,
                Instant.parse("2026-11-01T10:00:00Z"));
        accreditations.insert(Accreditation.from(submission));
        history.append(StatusChange.from(submission));

        var result = service.get(new GetAccreditationHistoryQuery(submission.accreditationId()));

        assertThat(result.userId()).isEqualTo(USER);
        assertThat(result.changes()).containsExactly(StatusChange.from(submission));
    }

    @Test
    void reportsAnUnknownAccreditation() {
        var query = new GetAccreditationHistoryQuery(AccreditationId.newId());

        assertThatThrownBy(() -> service.get(query)).isInstanceOf(AccreditationNotFoundException.class);
    }
}
