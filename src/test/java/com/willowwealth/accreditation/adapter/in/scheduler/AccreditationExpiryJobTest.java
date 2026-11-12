package com.willowwealth.accreditation.adapter.in.scheduler;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.domain.event.AccreditationStatusChanged;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.Counters.count;
import static com.willowwealth.accreditation.domain.event.ChangeOrigin.SYSTEM_EXPIRY;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// The scheduler is off in tests, so the test runs the job itself.
@QuarkusTest
class AccreditationExpiryJobTest {

    private static final String TOPIC = "accreditation-status-events";

    @Inject
    AccreditationExpiryJob job;

    @Inject
    AccreditationRepositoryImpl accreditations;

    @InjectSpy
    AccreditationEventPublisher publisher;

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @Inject
    MeterRegistry registry;

    @Test
    void expiresWhatWasConfirmedMoreThanThirtyDaysAgo() {
        var userId = UUID.randomUUID().toString().replace("-", "");
        var now = Instant.now();
        var expiring = UUID.randomUUID();
        var stillValid = UUID.randomUUID();
        var pending = UUID.randomUUID();
        // The events carry their own time, which the projection keeps as the last update.
        // They share the user as key, so they are applied in this order.
        kafka.produceStrings().fromRecords(
                record(userId, submission(expiring, userId, now.minus(Duration.ofDays(45)))),
                record(userId, confirmation(expiring, userId, now.minus(Duration.ofDays(31)))),
                record(userId, submission(stillValid, userId, now.minus(Duration.ofDays(35)))),
                record(userId, confirmation(stillValid, userId, now.minus(Duration.ofDays(29)))),
                record(userId, submission(pending, userId, now.minus(Duration.ofDays(40)))))
                .awaitCompletion();
        waitForStatus(pending, PENDING);
        var expiries = count(registry, "accreditation.expiries");

        job.expireConfirmedAccreditations();

        verify(publisher).publish(argThat(event -> event instanceof AccreditationStatusChanged change
                && change.accreditationId().value().equals(expiring)
                && change.previousStatus() == CONFIRMED
                && change.newStatus() == EXPIRED
                && change.origin() == SYSTEM_EXPIRY));
        verify(publisher, never()).publish(argThat(event -> event.accreditationId().value().equals(stillValid)));
        verify(publisher, never()).publish(argThat(event -> event.accreditationId().value().equals(pending)));
        // Old confirmed rows left by other tests can expire in the same run.
        assertThat(count(registry, "accreditation.expiries")).isGreaterThanOrEqualTo(expiries + 1);

        waitForStatus(expiring, EXPIRED);
        assertThat(statusOf(stillValid)).isEqualTo(CONFIRMED);
        assertThat(statusOf(pending)).isEqualTo(PENDING);
        var history = given().when().get("/api/v1/user/accreditation/{id}/history", expiring)
                .then().statusCode(200)
                .extract().jsonPath();
        assertThat(history.getList("history.origin", String.class)).containsExactly("ADMIN", "ADMIN", "SYSTEM_EXPIRY");
    }

    private void waitForStatus(UUID accreditationId, AccreditationStatus status) {
        await().atMost(Duration.ofSeconds(10)).until(() -> statusOf(accreditationId) == status);
    }

    private AccreditationStatus statusOf(UUID accreditationId) {
        return QuarkusTransaction.requiringNew().call(() -> accreditations.findByIdOptional(accreditationId)
                .map(AccreditationEntity::getStatus)
                .orElse(null));
    }

    private static ProducerRecord<String, String> record(String key, String value) {
        return new ProducerRecord<>(TOPIC, key, value);
    }

    private static String submission(UUID accreditationId, String userId, Instant occurredAt) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_SUBMITTED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "new_status": "PENDING", "origin": "ADMIN",
                 "occurred_at": "%s",
                 "document": {"document_id": "%s", "name": "2018.pdf", "mime_type": "application/pdf",
                              "size_bytes": 42, "sha256": "%s"}}"""
                .formatted(UUID.randomUUID(), accreditationId, userId, occurredAt, UUID.randomUUID(), "0".repeat(64));
    }

    private static String confirmation(UUID accreditationId, String userId, Instant occurredAt) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_STATUS_CHANGED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "previous_status": "PENDING",
                 "new_status": "CONFIRMED", "origin": "ADMIN", "occurred_at": "%s"}"""
                .formatted(UUID.randomUUID(), accreditationId, userId, occurredAt);
    }
}
