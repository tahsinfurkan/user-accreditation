package com.willowwealth.accreditation.adapter.in.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

// The other tests send requests faster than an admin endpoint allows, so their limits are set very high.
@QuarkusTest
@TestProfile(RateLimitTest.LowLimits.class)
class RateLimitTest {

    private static final String HISTORY = "/api/v1/user/accreditation/{accreditationId}/history";
    private static final String USER_ACCREDITATIONS = "/api/v1/user/{userId}/accreditation";

    @Test
    void answersTooManyRequestsOverTheLimitAndAcceptsRequestsAgainLater() {
        // An unknown id is enough: every call counts, whatever its answer.
        for (var i = 0; i < 3; i++) {
            history().statusCode(404);
        }

        var limited = history();
        assertErrorBody(limited, 429, "Too many requests");
        limited.header("Retry-After", "1");

        // Every endpoint has a limit of its own.
        given().when().get(USER_ACCREDITATIONS, newUserId()).then().statusCode(404);

        // Rejected calls count towards the rolling window as well, so the endpoint opens again only for a client that
        // keeps quiet for Retry-After. Polling every 100 ms, as Awaitility does by default, would keep it closed.
        await().pollDelay(Duration.ofSeconds(1)).pollInterval(Duration.ofMillis(500)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> history().statusCode(404));
    }

    private static ValidatableResponse history() {
        return given().when().get(HISTORY, UUID.randomUUID()).then();
    }

    // Three requests a second for each admin endpoint; the client endpoint keeps its high test limit.
    public static class LowLimits implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("accreditation.rate-limit.admin", "3");
        }
    }
}
