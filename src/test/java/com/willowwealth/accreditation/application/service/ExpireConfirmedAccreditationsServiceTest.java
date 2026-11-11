package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.in.command.ExpireConfirmedAccreditationsCommand;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.SYSTEM_EXPIRY;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.FAILED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;

class ExpireConfirmedAccreditationsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T09:00:00Z");
    private static final Duration TTL = Duration.ofDays(30);

    private final InMemoryAccreditationRepository accreditations = new InMemoryAccreditationRepository();
    private final List<AccreditationId> attempted = new ArrayList<>();
    private final List<AccreditationEvent> published = new ArrayList<>();
    private final Map<AccreditationId, RuntimeException> failures = new HashMap<>();
    private final ExpireConfirmedAccreditationsService service =
            new ExpireConfirmedAccreditationsService(accreditations, this::publish, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void expiresWhatWasConfirmedLongerAgoThanTheTtl() {
        var due = add(CONFIRMED, NOW.minus(Duration.ofDays(31)));
        add(CONFIRMED, NOW.minus(Duration.ofDays(29)));
        add(PENDING, NOW.minus(Duration.ofDays(40)));
        add(FAILED, NOW.minus(Duration.ofDays(40)));
        add(EXPIRED, NOW.minus(Duration.ofDays(40)));

        var expiries = service.expire(command(500));

        assertThat(expiries).isEqualTo(1);
        assertThat(published).singleElement().isInstanceOfSatisfying(AccreditationStatusChanged.class, change -> {
            assertThat(change.accreditationId()).isEqualTo(due.id());
            assertThat(change.previousStatus()).isEqualTo(CONFIRMED);
            assertThat(change.newStatus()).isEqualTo(EXPIRED);
            assertThat(change.origin()).isEqualTo(SYSTEM_EXPIRY);
            assertThat(change.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void goesThroughEveryPage() {
        var lastUpdate = NOW.minus(Duration.ofDays(31));
        // Two of them share an update time, so the id decides where a page ends.
        var due = List.of(add(CONFIRMED, lastUpdate.minusSeconds(2)), add(CONFIRMED, lastUpdate.minusSeconds(1)),
                add(CONFIRMED, lastUpdate), add(CONFIRMED, lastUpdate), add(CONFIRMED, lastUpdate.plusSeconds(1)));

        var expiries = service.expire(command(2));

        assertThat(expiries).isEqualTo(5);
        assertThat(published).extracting(AccreditationEvent::accreditationId)
                .containsExactlyInAnyOrderElementsOf(due.stream().map(Accreditation::id).toList());
    }

    @Test
    void keepsGoingWhenOneAccreditationCannotBeExpired() {
        var broken = add(CONFIRMED, NOW.minus(Duration.ofDays(32)));
        var fine = add(CONFIRMED, NOW.minus(Duration.ofDays(31)));
        failures.put(broken.id(), new IllegalStateException("Unexpected failure"));

        var expiries = service.expire(command(500));

        assertThat(expiries).isEqualTo(1);
        assertThat(published).extracting(AccreditationEvent::accreditationId).containsExactly(fine.id());
    }

    @Test
    void stopsWhenKafkaIsUnreachable() {
        var first = add(CONFIRMED, NOW.minus(Duration.ofDays(33)));
        var second = add(CONFIRMED, NOW.minus(Duration.ofDays(32)));
        add(CONFIRMED, NOW.minus(Duration.ofDays(31)));
        failures.put(second.id(), new EventPublicationException("Not acknowledged in time", new RuntimeException()));

        var expiries = service.expire(command(500));

        assertThat(expiries).isEqualTo(1);
        assertThat(attempted).containsExactly(first.id(), second.id());
    }

    private void publish(AccreditationEvent event) {
        attempted.add(event.accreditationId());
        var failure = failures.get(event.accreditationId());
        if (failure != null) {
            throw failure;
        }
        published.add(event);
    }

    private Accreditation add(AccreditationStatus status, Instant updatedAt) {
        var accreditation = Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(new UserId(UUID.randomUUID().toString().replace("-", "")))
                .type(BY_INCOME)
                .status(status)
                .documentId(UUID.randomUUID())
                .createdAt(updatedAt.minus(Duration.ofDays(1)))
                .updatedAt(updatedAt)
                .build();
        accreditations.insert(accreditation);
        return accreditation;
    }

    private static ExpireConfirmedAccreditationsCommand command(int batchSize) {
        return new ExpireConfirmedAccreditationsCommand(TTL, batchSize);
    }
}
