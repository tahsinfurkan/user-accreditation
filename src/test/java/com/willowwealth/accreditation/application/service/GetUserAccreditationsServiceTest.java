package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.port.in.query.GetUserAccreditationsQuery;
import com.willowwealth.accreditation.domain.exception.UserNotFoundException;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetUserAccreditationsServiceTest {

    private static final UserId USER = new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V");

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final GetUserAccreditationsService service = new GetUserAccreditationsService(accreditations);

    @Test
    void returnsTheUsersAccreditations() {
        var accreditation = pendingAccreditation();
        accreditations.insert(accreditation);

        var result = service.get(new GetUserAccreditationsQuery(USER));

        assertThat(result).containsExactly(new AccreditationSummary(accreditation.id(), BY_INCOME, PENDING));
    }

    @Test
    void reportsAUserWithoutAccreditations() {
        assertThatThrownBy(() -> service.get(new GetUserAccreditationsQuery(USER)))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static Accreditation pendingAccreditation() {
        var now = Instant.parse("2026-11-01T10:00:00Z");
        return Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(USER)
                .type(BY_INCOME)
                .status(PENDING)
                .documentId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
