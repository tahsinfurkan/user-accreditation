package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

// Calls and waits that several endpoint tests need.
final class AccreditationRequests {

    private static final String SUBMIT = "/api/v1/user/accreditation";
    private static final String ACCREDITATION = "/api/v1/user/accreditation/{accreditationId}";

    private AccreditationRequests() {
    }

    static String newUserId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    static UUID submit(String userId, String type) {
        var id = given().contentType(ContentType.JSON).body("""
                        {"user_id": "%s", "accreditation_type": "%s",
                         "document": {"name": "2021.pdf", "mime_type": "application/pdf", "content": "JVBERi0xLjcK"}}"""
                        .formatted(userId, type))
                .when().post(SUBMIT)
                .then().statusCode(202)
                .extract().jsonPath().getString("accreditation_id");
        return UUID.fromString(id);
    }

    static ValidatableResponse putOutcome(UUID accreditationId, String outcome) {
        return given().contentType(ContentType.JSON).body("""
                        {"outcome": "%s"}""".formatted(outcome))
                .when().put(ACCREDITATION, accreditationId)
                .then();
    }

    // Changes are applied by the consumer, so tests wait until the database shows them.
    static void waitForStatus(AccreditationRepositoryImpl accreditations, UUID accreditationId,
                              AccreditationStatus status) {
        await().atMost(Duration.ofSeconds(10)).until(() -> QuarkusTransaction.requiringNew()
                .call(() -> accreditations.findByIdOptional(accreditationId).map(AccreditationEntity::getStatus))
                .filter(status::equals)
                .isPresent());
    }
}
