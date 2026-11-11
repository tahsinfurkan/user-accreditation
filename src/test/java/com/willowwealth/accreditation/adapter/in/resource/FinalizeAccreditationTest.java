package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.UserId;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.CONFIRMED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.FAILED;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class FinalizeAccreditationTest {

    private static final String SUBMIT = "/api/v1/user/accreditation";
    private static final String ACCREDITATION = "/api/v1/user/accreditation/{accreditationId}";
    private static final String STATUSES = "/api/v1/user/{userId}/accreditation";
    private static final String TOPIC = "accreditation-status-events";

    @Inject
    AccreditationRepositoryImpl accreditations;

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @Test
    void confirmsAnAccreditationAndLaterExpiresIt() {
        var id = submitAndWait(newUserId(), "BY_INCOME");

        putOutcome(id, "CONFIRMED").statusCode(202).body("accreditation_id", equalTo(id.toString()));
        waitForStatus(id, CONFIRMED);

        putOutcome(id, "EXPIRED").statusCode(202);
        waitForStatus(id, EXPIRED);
    }

    @Test
    void failsAPendingAccreditation() {
        var id = submitAndWait(newUserId(), "BY_INCOME");

        putOutcome(id, "FAILED").statusCode(202);

        waitForStatus(id, FAILED);
    }

    @Test
    void listsFailedAndConfirmedAccreditationsLikeTheSpecExample() {
        var userId = newUserId();
        var failed = submitAndWait(userId, "BY_INCOME");
        putOutcome(failed, "FAILED").statusCode(202);
        waitForStatus(failed, FAILED);
        var confirmed = submitAndWait(userId, "BY_NET_WORTH");
        putOutcome(confirmed, "CONFIRMED").statusCode(202);
        waitForStatus(confirmed, CONFIRMED);

        Map<String, Map<String, String>> statuses = given().when().get(STATUSES, userId)
                .then().statusCode(200)
                .extract().jsonPath().getMap("accreditation_statuses");

        assertThat(statuses).containsExactly(
                entry(failed.toString(), Map.of("accreditation_type", "BY_INCOME", "status", "FAILED")),
                entry(confirmed.toString(), Map.of("accreditation_type", "BY_NET_WORTH", "status", "CONFIRMED")));
    }

    @Test
    void publishesTheStatusChangeKeyedByUser() {
        var userId = newUserId();
        var id = submitAndWait(userId, "BY_INCOME");

        putOutcome(id, "CONFIRMED").statusCode(202);

        var record = kafka.consumeStrings()
                .withOffsetReset("earliest")
                .fromTopics(TOPIC, records -> records
                        .filter(r -> userId.equals(r.key()) && r.value().contains("ACCREDITATION_STATUS_CHANGED"))
                        .select().first())
                .awaitCompletion(Duration.ofSeconds(10))
                .getFirstRecord();
        var event = new JsonPath(record.value());
        assertThat(event.getString("accreditation_id")).isEqualTo(id.toString());
        assertThat(event.getString("previous_status")).isEqualTo("PENDING");
        assertThat(event.getString("new_status")).isEqualTo("CONFIRMED");
        assertThat(event.getString("origin")).isEqualTo("ADMIN");
        assertThat(event.getString("document")).isNull();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
            PENDING   | EXPIRED   | A PENDING accreditation can only be changed to CONFIRMED or FAILED.
            CONFIRMED | CONFIRMED | A CONFIRMED accreditation can only be changed to EXPIRED.
            CONFIRMED | FAILED    | A CONFIRMED accreditation can only be changed to EXPIRED.
            FAILED    | CONFIRMED | A FAILED accreditation can no longer be updated.
            FAILED    | EXPIRED   | A FAILED accreditation can no longer be updated.
            FAILED    | FAILED    | A FAILED accreditation can no longer be updated.
            EXPIRED   | CONFIRMED | An EXPIRED accreditation can no longer be updated.
            EXPIRED   | EXPIRED   | An EXPIRED accreditation can no longer be updated.
            EXPIRED   | FAILED    | An EXPIRED accreditation can no longer be updated.
            """)
    void rejectsAChangeTheCurrentStatusDoesNotAllow(AccreditationStatus current, String outcome, String detail) {
        var id = existingAccreditation(current);

        var error = assertErrorBody(putOutcome(id, outcome), 409, "Status change not allowed");

        assertThat(error.getString("detail")).isEqualTo(detail);
    }

    @Test
    void answersAnUnknownAccreditationWithNotFound() {
        assertErrorBody(putOutcome(UUID.randomUUID(), "CONFIRMED"), 404, "Accreditation not found");
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() {
        var response = given().contentType(ContentType.JSON).body("""
                        {"outcome": "CONFIRMED"}""")
                .when().put(ACCREDITATION, "not-a-uuid")
                .then();

        var error = assertErrorBody(response, 400, "Invalid request");
        assertThat(error.getList("errors.field", String.class)).containsExactly("accreditationId");
    }

    @Test
    void rejectsPendingAsAnOutcome() {
        var error = assertErrorBody(putOutcome(UUID.randomUUID(), "PENDING"), 400, "Invalid request");

        assertThat(error.getList("errors.field", String.class)).containsExactly("outcome");
    }

    private ValidatableResponse putOutcome(UUID accreditationId, String outcome) {
        return given().contentType(ContentType.JSON).body("""
                        {"outcome": "%s"}""".formatted(outcome))
                .when().put(ACCREDITATION, accreditationId)
                .then();
    }

    private UUID submitAndWait(String userId, String type) {
        var id = given().contentType(ContentType.JSON).body("""
                        {"user_id": "%s", "accreditation_type": "%s",
                         "document": {"name": "2021.pdf", "mime_type": "application/pdf", "content": "JVBERi0xLjcK"}}"""
                        .formatted(userId, type))
                .when().post(SUBMIT)
                .then().statusCode(202)
                .extract().jsonPath().getString("accreditation_id");
        var accreditationId = UUID.fromString(id);
        waitForStatus(accreditationId, PENDING);
        return accreditationId;
    }

    // Changes are applied by the consumer, so tests wait until the database shows them.
    private void waitForStatus(UUID accreditationId, AccreditationStatus status) {
        await().atMost(Duration.ofSeconds(10)).until(() -> QuarkusTransaction.requiringNew()
                .call(() -> accreditations.findByIdOptional(accreditationId).map(AccreditationEntity::getStatus))
                .filter(status::equals)
                .isPresent());
    }

    private UUID existingAccreditation(AccreditationStatus status) {
        var now = Instant.now();
        var accreditation = Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(new UserId(newUserId()))
                .type(BY_INCOME)
                .status(status)
                .documentId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();
        QuarkusTransaction.requiringNew().run(() -> accreditations.insert(accreditation));
        return accreditation.id().value();
    }

    private static String newUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
