package com.willowwealth.accreditation.adapter.out.persistence;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.mapper.AccreditationEntityMapper;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.putOutcome;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.submit;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.waitForStatus;
import static com.willowwealth.accreditation.config.CircuitBreakerProducer.DATABASE;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static io.restassured.RestAssured.given;
import static io.smallrye.faulttolerance.api.CircuitBreakerState.CLOSED;
import static io.smallrye.faulttolerance.api.CircuitBreakerState.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

// In tests the circuit opens after 4 calls and stays open for 2 seconds; the consumer retries 0.4, 0.8 and 1.6 s apart.
@QuarkusTest
class DatabaseCircuitBreakerTest {

    private static final String TOPIC = "accreditation-status-events";

    // Breaks reading an accreditation inside the repository, where the database would fail.
    @InjectSpy
    AccreditationEntityMapper entityMapper;

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Inject
    CircuitBreakerMaintenance circuitBreakers;

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @AfterEach
    void closeTheCircuits() {
        circuitBreakers.resetAll();
    }

    @Test
    void turnsRequestsAwayWhileTheDatabaseIsDownAndLetsThemThroughOnceItIsBack() {
        var userId = newUserId();
        var id = submit(userId, "BY_INCOME");
        waitForStatus(accreditations, id, PENDING);
        given().when().get("/api/v1/user/{userId}/accreditation", userId).then().statusCode(200);

        breakTheDatabase(id);

        var turnedAway = putOutcome(id, "CONFIRMED");
        assertErrorBody(turnedAway, 503, "Service temporarily unavailable");
        turnedAway.header("Retry-After", "2");
        // One circuit for the whole database, so the history repository turns requests away as well.
        assertErrorBody(given().when().get("/api/v1/user/accreditation/{id}/history", id).then(), 503,
                "Service temporarily unavailable");
        // A cached read never reaches the database.
        given().when().get("/api/v1/user/{userId}/accreditation", userId).then().statusCode(200);

        repairTheDatabase();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> putOutcome(id, "CONFIRMED").statusCode(202));
        assertThat(circuitBreakers.currentState(DATABASE)).isEqualTo(CLOSED);
    }

    @Test
    void appliesAnEventThatArrivesWhileTheCircuitIsOpen() {
        var userId = newUserId();
        var id = submit(userId, "BY_INCOME");
        waitForStatus(accreditations, id, PENDING);
        breakTheDatabase(id);
        repairTheDatabase();

        // The first attempts are turned away, but the retries outlast the open circuit: once it lets a trial call
        // through, the event is applied instead of going to the dead-letter topic.
        kafka.produceStrings()
                .fromRecords(new ProducerRecord<>(TOPIC, userId, confirmation(id, userId)))
                .awaitCompletion();

        waitForStatus(accreditations, id, CONFIRMED);
    }

    // Every attempt to change the accreditation fails until the circuit opens and turns the next one away.
    private void breakTheDatabase(UUID accreditationId) {
        doThrow(new IllegalStateException("Database unavailable"))
                .when(entityMapper).toDomain(any(AccreditationEntity.class));
        await().atMost(Duration.ofSeconds(5))
                .until(() -> putOutcome(accreditationId, "CONFIRMED").extract().statusCode() == 503);
        assertThat(circuitBreakers.currentState(DATABASE)).isEqualTo(OPEN);
    }

    private void repairTheDatabase() {
        doCallRealMethod().when(entityMapper).toDomain(any(AccreditationEntity.class));
    }

    private static String confirmation(UUID accreditationId, String userId) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_STATUS_CHANGED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "previous_status": "PENDING",
                 "new_status": "CONFIRMED", "origin": "ADMIN", "occurred_at": "%s"}"""
                .formatted(UUID.randomUUID(), accreditationId, userId, Instant.now());
    }
}
