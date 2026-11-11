package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class UserAccreditationResourceTest {

    private static final String SUBMIT = "/api/v1/user/accreditation";
    private static final String STATUSES = "/api/v1/user/{userId}/accreditation";

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Test
    void showsASubmissionAsPendingOnceItIsRecorded() {
        var userId = newUserId();
        var accreditationId = given().contentType(ContentType.JSON).body("""
                        {"user_id": "%s", "accreditation_type": "BY_NET_WORTH",
                         "document": {"name": "2021.pdf", "mime_type": "application/pdf", "content": "JVBERi0xLjcK"}}"""
                        .formatted(userId))
                .when().post(SUBMIT)
                .then().statusCode(202)
                .extract().jsonPath().getString("accreditation_id");

        // Waiting on the database rather than polling the endpoint: an early poll would put the empty result in
        // the cache, and it would only go away once the consumer's eviction lands.
        await().atMost(Duration.ofSeconds(10)).until(() -> QuarkusTransaction.requiringNew()
                .call(() -> accreditations.findById(UUID.fromString(accreditationId))) != null);

        var body = given().when().get(STATUSES, userId)
                .then().statusCode(200).contentType(ContentType.JSON)
                .extract().jsonPath();
        Map<String, Map<String, String>> statuses = body.getMap("accreditation_statuses");

        assertThat(body.getString("user_id")).isEqualTo(userId);
        assertThat(statuses).containsExactly(
                entry(accreditationId, Map.of("accreditation_type", "BY_NET_WORTH", "status", "PENDING")));
    }

    @Test
    void answersWithNotFoundForAUserWithoutAccreditations() {
        assertErrorBody(given().when().get(STATUSES, newUserId()).then(), 404, "User not found");
    }

    @Test
    void rejectsAUserIdThatIsNotThirtyTwoLettersOrDigits() {
        var error = assertErrorBody(given().when().get(STATUSES, "not-a-user-id").then(), 400, "Invalid request");

        assertThat(error.getList("errors.field", String.class)).containsExactly("userId");
    }

    private static String newUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
