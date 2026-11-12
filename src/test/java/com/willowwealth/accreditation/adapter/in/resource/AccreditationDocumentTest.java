package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.submit;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.waitForStatus;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AccreditationDocumentTest {

    private static final String DOCUMENT = "/api/v1/user/accreditation/{accreditationId}/document";

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Test
    void returnsTheDocumentTheAccreditationWasSubmittedWith() {
        var id = submit(newUserId(), "BY_INCOME");
        waitForStatus(accreditations, id, PENDING);

        var content = given().when().get(DOCUMENT, id)
                .then().statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", "attachment; filename*=UTF-8''2021.pdf")
                .extract().asByteArray();

        assertThat(content).isEqualTo(Base64.getDecoder().decode("JVBERi0xLjcK"));
    }

    @Test
    void answersAnUnknownAccreditationWithNotFound() {
        assertErrorBody(given().when().get(DOCUMENT, UUID.randomUUID()).then(), 404, "Accreditation not found");
    }

    @Test
    void rejectsAnIdThatIsNotAUuid() {
        assertErrorBody(given().when().get(DOCUMENT, "not-a-uuid").then(), 400, "Invalid request");
    }
}
