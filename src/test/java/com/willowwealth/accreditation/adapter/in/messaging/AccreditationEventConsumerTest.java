package com.willowwealth.accreditation.adapter.in.messaging;

import com.willowwealth.accreditation.adapter.messaging.mapper.AccreditationEventMessageMapper;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.application.port.in.usecase.ApplyAccreditationEventUseCase;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
class AccreditationEventConsumerTest {

    private static final String TOPIC = "accreditation-status-events";
    private static final String DEAD_LETTER_TOPIC = "accreditation-status-events-dlq";

    // Spies run the real code; a test makes them fail only for the event it sends.
    @InjectSpy
    ApplyAccreditationEventUseCase applyAccreditationEvent;

    @InjectSpy
    AccreditationEventMessageMapper mapper;

    @Inject
    AccreditationRepositoryImpl accreditations;

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @Test
    void retriesAFailedEventUntilItIsApplied() {
        var userId = newUserId();
        var eventId = UUID.randomUUID();
        var accreditationId = UUID.randomUUID();
        doThrow(new IllegalStateException("Database unavailable"))
                .doThrow(new IllegalStateException("Database unavailable"))
                .doCallRealMethod()
                .when(applyAccreditationEvent).apply(argThat(event -> eventId.equals(event.eventId())));

        produce(userId, submission(eventId, accreditationId, userId));

        await().atMost(Duration.ofSeconds(10)).until(() -> QuarkusTransaction.requiringNew()
                .call(() -> accreditations.findByIdOptional(accreditationId).isPresent()));
        verify(applyAccreditationEvent, times(3)).apply(argThat(event -> eventId.equals(event.eventId())));
        assertNothingDeadLetteredFor(userId);
    }

    @Test
    void deadLettersAnEventThatStillFailsAfterThreeRetries() {
        var userId = newUserId();
        var eventId = UUID.randomUUID();
        var accreditationId = UUID.randomUUID();
        doThrow(new IllegalStateException("Database unavailable"))
                .when(applyAccreditationEvent).apply(argThat(event -> eventId.equals(event.eventId())));

        produce(userId, submission(eventId, accreditationId, userId));

        var deadLetter = firstDeadLetterFor(userId);
        assertThat(deadLetter.value()).contains(eventId.toString());
        assertThat(header(deadLetter, "dead-letter-reason")).isEqualTo("Database unavailable");
        assertThat(header(deadLetter, "dead-letter-topic")).isEqualTo(TOPIC);
        assertThat(header(deadLetter, "dead-letter-partition")).isNotBlank();
        assertThat(header(deadLetter, "dead-letter-offset")).isNotBlank();
        verify(applyAccreditationEvent, times(4)).apply(argThat(event -> eventId.equals(event.eventId())));
        assertThat(QuarkusTransaction.requiringNew().call(() -> accreditations.findByIdOptional(accreditationId)))
                .isEmpty();
    }

    @Test
    void deadLettersAnEventThatCannotBeMappedWithoutRetrying() {
        var key = newUserId();
        var eventId = UUID.randomUUID();

        // Valid JSON, but the user id has the wrong format, so the message never becomes an event.
        produce(key, submission(eventId, UUID.randomUUID(), "not-a-user-id"));

        assertThat(header(firstDeadLetterFor(key), "dead-letter-reason")).contains("user id");
        verify(mapper, times(1)).toEvent(argThat(message -> eventId.equals(message.eventId())));
    }

    @Test
    void deadLettersUnreadableJsonAndStaysHealthy() {
        var key = newUserId();

        produce(key, "{\"event_id\": ");

        assertThat(firstDeadLetterFor(key).value()).isEqualTo("{\"event_id\": ");
        given().when().get("/q/health").then().statusCode(200);
    }

    @Test
    void doesNotDeadLetterARejectedEvent() {
        var userId = newUserId();
        var eventId = UUID.randomUUID();

        // The accreditation does not exist, so the projection rejects the change.
        produce(userId, statusChange(eventId, UUID.randomUUID(), userId));

        assertNothingDeadLetteredFor(userId);
        verify(applyAccreditationEvent, times(1)).apply(argThat(event -> eventId.equals(event.eventId())));
    }

    // The marker is not JSON, so it goes to the dead-letter topic without retries. It shares the key, so it is
    // handled after everything sent before it: if it is the key's first dead letter, none of those got there.
    private void assertNothingDeadLetteredFor(String key) {
        var marker = "not json " + UUID.randomUUID();
        produce(key, marker);
        assertThat(firstDeadLetterFor(key).value()).isEqualTo(marker);
    }

    private void produce(String key, String value) {
        kafka.produceStrings().fromRecords(new ProducerRecord<>(TOPIC, key, value)).awaitCompletion();
    }

    private ConsumerRecord<String, String> firstDeadLetterFor(String key) {
        return kafka.consumeStrings()
                .withOffsetReset("earliest")
                .fromTopics(DEAD_LETTER_TOPIC, records -> records.filter(r -> key.equals(r.key())).select().first())
                .awaitCompletion(Duration.ofSeconds(15))
                .getFirstRecord();
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String newUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String submission(UUID eventId, UUID accreditationId, String userId) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_SUBMITTED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "new_status": "PENDING", "origin": "ADMIN",
                 "occurred_at": "%s",
                 "document": {"document_id": "%s", "name": "2018.pdf", "mime_type": "application/pdf",
                              "size_bytes": 42, "sha256": "%s"}}"""
                .formatted(eventId, accreditationId, userId, Instant.now(), UUID.randomUUID(), "0".repeat(64));
    }

    private static String statusChange(UUID eventId, UUID accreditationId, String userId) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_STATUS_CHANGED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "previous_status": "PENDING",
                 "new_status": "CONFIRMED", "origin": "ADMIN", "occurred_at": "%s"}"""
                .formatted(eventId, accreditationId, userId, Instant.now());
    }
}
