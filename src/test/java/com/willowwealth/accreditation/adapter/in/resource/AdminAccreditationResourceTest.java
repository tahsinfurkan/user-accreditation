package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.adapter.out.persistence.repository.DocumentRepositoryImpl;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.Document;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
class AdminAccreditationResourceTest {

    private static final String ENDPOINT = "/api/v1/user/accreditation";
    private static final String TOPIC = "accreditation-status-events";

    // The document from the spec's example. It is called 2018.pdf but is really a few bytes of JSON.
    private static final String SPEC_CONTENT = "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==";
    private static final String DOCUMENT = """
            {"name": "2018.pdf", "mime_type": "application/pdf", "content": "%s"}""".formatted(SPEC_CONTENT);

    @InjectKafkaCompanion
    KafkaCompanion kafka;

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Inject
    DocumentRepositoryImpl documents;

    @Test
    void recordsTheSubmissionAsPending() {
        var userId = randomUserId();

        var accreditationId = submit(userId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var accreditation = QuarkusTransaction.requiringNew().call(() -> accreditations.findById(accreditationId));
            assertThat(accreditation).isNotNull();
            assertThat(accreditation.getUserId()).isEqualTo(userId);
            assertThat(accreditation.getStatus()).isEqualTo(AccreditationStatus.PENDING);
        });
    }

    @Test
    void storesTheDocumentSeparately() {
        var accreditationId = submit(randomUserId());

        var documentId = await().atMost(Duration.ofSeconds(10)).until(
                () -> QuarkusTransaction.requiringNew().call(() -> accreditations.findById(accreditationId)),
            Objects::nonNull).getDocumentId();
        var document = QuarkusTransaction.requiringNew().call(() -> documents.findById(documentId));

        var expected = Base64.getDecoder().decode(SPEC_CONTENT);
        assertThat(document.getContent()).isEqualTo(expected);
        assertThat(document.getSizeBytes()).isEqualTo(expected.length);
        assertThat(document.getSha256()).hasSize(64);
    }

    @Test
    void publishesTheSubmissionKeyedByUser() {
        var userId = randomUserId();
        var accreditationId = submit(userId);

        var record = kafka.consumeStrings()
                .withOffsetReset("earliest")
                .fromTopics(TOPIC, records -> records.filter(r -> userId.equals(r.key())).select().first())
                .awaitCompletion(Duration.ofSeconds(10))
                .getFirstRecord();

        var event = new JsonPath(record.value());
        assertThat(event.getString("event_type")).isEqualTo("ACCREDITATION_SUBMITTED");
        assertThat(event.getString("accreditation_id")).isEqualTo(accreditationId.toString());
        assertThat(event.getString("new_status")).isEqualTo("PENDING");
        assertThat(event.getString("origin")).isEqualTo("ADMIN");
        assertThat(event.getString("document.sha256")).hasSize(64);
        // Only a reference to the document travels through Kafka, never the file itself.
        assertThat(record.value()).doesNotContain(SPEC_CONTENT);
    }

    @Test
    void rejectsASecondSubmissionWhileTheFirstIsPending() {
        var userId = randomUserId();
        var first = submit(userId);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> QuarkusTransaction.requiringNew().call(() -> accreditations.findById(first)) != null);

        var response = given().contentType(ContentType.JSON).body(request(userId))
                .when().post(ENDPOINT)
                .then();

        assertErrorBody(response, 409, "Accreditation already pending");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestsWithAnInvalidField")
    void namesTheFieldThatBreaksTheContract(String description, String body, String field) {
        var response = given().contentType(ContentType.JSON).body(body)
                .when().post(ENDPOINT)
                .then();

        var error = assertErrorBody(response, 400, "Invalid request");
        assertThat(error.getList("errors.field", String.class)).containsExactly(field);
        assertThat(error.getList("errors.message", String.class)).allSatisfy(message -> assertThat(message).isNotBlank());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bodiesThatAreNotValidJson")
    void rejectsBodiesThatAreNotValidJson(String description, String body) {
        var response = given().contentType(ContentType.JSON).body(body)
                .when().post(ENDPOINT)
                .then();

        var error = assertErrorBody(response, 400, "Invalid request");
        assertThat(error.getString("detail")).isEqualTo("The request body is not valid JSON. Please send a valid JSON object.");
        assertThat(error.getList("errors")).isNull();
    }

    @Test
    void rejectsBodiesThatAreNotJson() {
        var response = given().contentType(ContentType.TEXT).body("hello")
                .when().post(ENDPOINT)
                .then();

        assertErrorBody(response, 415, "Unsupported media type");
    }

    static Stream<Arguments> requestsWithAnInvalidField() {
        var userId = randomUserId();
        var tooLarge = Base64.getEncoder().encodeToString(new byte[Document.MAX_SIZE_BYTES + 1]);
        return Stream.of(
                arguments("missing user_id", """
                        {"accreditation_type": "BY_INCOME", "document": %s}""".formatted(DOCUMENT), "user_id"),
                arguments("null user_id", """
                        {"user_id": null, "accreditation_type": "BY_INCOME", "document": %s}""".formatted(DOCUMENT),
                        "user_id"),
                arguments("user_id given as a number", """
                        {"user_id": 123, "accreditation_type": "BY_INCOME", "document": %s}""".formatted(DOCUMENT),
                        "user_id"),
                arguments("user_id one character short", """
                        {"user_id": "%s", "accreditation_type": "BY_INCOME", "document": %s}"""
                        .formatted(userId.substring(1), DOCUMENT), "user_id"),
                arguments("user_id with a dash", """
                        {"user_id": "%s", "accreditation_type": "BY_INCOME", "document": %s}"""
                        .formatted("-" + userId.substring(1), DOCUMENT), "user_id"),
                arguments("unknown accreditation_type", """
                        {"user_id": "%s", "accreditation_type": "BY_SALARY", "document": %s}""".formatted(userId, DOCUMENT),
                        "accreditation_type"),
                arguments("unknown field", """
                        {"user_id": "%s", "accreditation_type": "BY_INCOME", "document": %s, "note": "x"}"""
                        .formatted(userId, DOCUMENT), "note"),
                arguments("missing document", """
                        {"user_id": "%s", "accreditation_type": "BY_INCOME"}""".formatted(userId), "document"),
                arguments("empty document name", requestWithDocument(userId, "", "application/pdf", SPEC_CONTENT),
                        "document.name"),
                arguments("document name over 255 characters",
                        requestWithDocument(userId, "a".repeat(256), "application/pdf", SPEC_CONTENT), "document.name"),
                arguments("mime_type other than pdf", requestWithDocument(userId, "2018.png", "image/png", SPEC_CONTENT),
                        "document.mime_type"),
                arguments("content that is not Base64",
                        requestWithDocument(userId, "2018.pdf", "application/pdf", "not base64!"), "document.content"),
                arguments("content over 5 MB", requestWithDocument(userId, "2018.pdf", "application/pdf", tooLarge),
                        "document.content"),
                arguments("empty body", "", "body"));
    }

    static Stream<Arguments> bodiesThatAreNotValidJson() {
        var userId = randomUserId();
        return Stream.of(
                arguments("malformed JSON", """
                        {"user_id": "%s", """.formatted(userId)),
                arguments("data after the JSON object", request(userId) + " {}"),
                arguments("duplicate key", """
                        {"user_id": "%s", "user_id": "%s", "accreditation_type": "BY_INCOME", "document": %s}"""
                        .formatted(userId, userId, DOCUMENT)));
    }

    private static UUID submit(String userId) {
        var id = given().contentType(ContentType.JSON).body(request(userId))
                .when().post(ENDPOINT)
                .then().statusCode(202)
                .extract().jsonPath().getString("accreditation_id");
        return UUID.fromString(id);
    }

    private static String request(String userId) {
        return """
                {"user_id": "%s", "accreditation_type": "BY_INCOME", "document": %s}""".formatted(userId, DOCUMENT);
    }

    private static String requestWithDocument(String userId, String name, String mimeType, String content) {
        return """
                {"user_id": "%s", "accreditation_type": "BY_INCOME",
                 "document": {"name": "%s", "mime_type": "%s", "content": "%s"}}"""
                .formatted(userId, name, mimeType, content);
    }

    private static String randomUserId() {
        var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        return ThreadLocalRandom.current().ints(32, 0, chars.length())
                .mapToObj(i -> String.valueOf(chars.charAt(i)))
                .collect(Collectors.joining());
    }
}
