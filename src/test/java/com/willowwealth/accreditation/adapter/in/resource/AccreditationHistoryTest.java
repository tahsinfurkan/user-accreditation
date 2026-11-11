package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.putOutcome;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.submit;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.waitForStatus;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AccreditationHistoryTest {

    private static final String HISTORY = "/api/v1/user/accreditation/{accreditationId}/history";
    private static final String TOPIC = "accreditation-status-events";

    @Inject
    AccreditationRepositoryImpl accreditations;

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @Test
    void listsEveryChangeStartingWithTheSubmission() {
        var userId = newUserId();
        var id = submit(userId, "BY_INCOME");
        waitForStatus(accreditations, id, PENDING);
        putOutcome(id, "CONFIRMED").statusCode(202);
        waitForStatus(accreditations, id, CONFIRMED);
        putOutcome(id, "EXPIRED").statusCode(202);
        waitForStatus(accreditations, id, EXPIRED);

        var history = historyOf(id);

        assertThat(history.getString("accreditation_id")).isEqualTo(id.toString());
        assertThat(history.getString("user_id")).isEqualTo(userId);
        assertThat(history.<String>getList("history.previous_status")).containsExactly(null, "PENDING", "CONFIRMED");
        assertThat(history.getList("history.new_status", String.class)).containsExactly("PENDING", "CONFIRMED", "EXPIRED");
        assertThat(history.getList("history.origin", String.class)).containsOnly("ADMIN");
        assertThat(history.getList("history.event_id", String.class)).doesNotHaveDuplicates();
    }

    @Test
    void recordsAnEventDeliveredTwiceOnlyOnce() {
        var userId = newUserId();
        var id = submit(userId, "BY_INCOME");
        waitForStatus(accreditations, id, PENDING);
        var now = Instant.now();
        var confirm = statusChange(id, userId, "PENDING", "CONFIRMED", now);
        var expire = statusChange(id, userId, "CONFIRMED", "EXPIRED", now.plusSeconds(1));

        // Kafka can deliver a message again, for example after a consumer restart. All three share the user as key,
        // so they land on one partition and EXPIRED shows up only after both copies have been handled.
        kafka.produceStrings()
                .fromRecords(new ProducerRecord<>(TOPIC, userId, confirm), new ProducerRecord<>(TOPIC, userId, confirm),
                        new ProducerRecord<>(TOPIC, userId, expire))
                .awaitCompletion();
        waitForStatus(accreditations, id, EXPIRED);

        assertThat(historyOf(id).getList("history.new_status", String.class))
                .containsExactly("PENDING", "CONFIRMED", "EXPIRED");
    }

    @Test
    void answersAnUnknownAccreditationWithNotFound() {
        assertErrorBody(given().when().get(HISTORY, UUID.randomUUID()).then(), 404, "Accreditation not found");
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() {
        var error = assertErrorBody(given().when().get(HISTORY, "not-a-uuid").then(), 400, "Invalid request");

        assertThat(error.getList("errors.field", String.class)).containsExactly("accreditationId");
    }

    private static JsonPath historyOf(UUID accreditationId) {
        return given().when().get(HISTORY, accreditationId)
                .then().statusCode(200).contentType(ContentType.JSON)
                .extract().jsonPath();
    }

    private static String statusChange(UUID accreditationId, String userId, String from, String to, Instant occurredAt) {
        return """
                {"event_id": "%s", "event_type": "ACCREDITATION_STATUS_CHANGED", "accreditation_id": "%s",
                 "user_id": "%s", "accreditation_type": "BY_INCOME", "previous_status": "%s", "new_status": "%s",
                 "origin": "ADMIN", "occurred_at": "%s"}"""
                .formatted(UUID.randomUUID(), accreditationId, userId, from, to, occurredAt);
    }
}
