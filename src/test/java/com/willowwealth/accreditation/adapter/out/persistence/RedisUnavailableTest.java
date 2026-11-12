package com.willowwealth.accreditation.adapter.out.persistence;

import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.UserId;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;

// Redis is only a cache: without it the client endpoint reads from PostgreSQL, and the service still counts as ready.
@QuarkusTest
@TestProfile(RedisUnavailableTest.RedisUnreachable.class)
class RedisUnavailableTest {

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Test
    void servesTheClientFromTheDatabase() {
        var userId = newUserId();
        var accreditation = pending(userId);
        // Evicting the cache after the commit fails as well, which must leave the insert in place.
        QuarkusTransaction.requiringNew().run(() -> accreditations.insert(accreditation));

        given().when().get("/api/v1/user/{userId}/accreditation", userId)
                .then().statusCode(200)
                .body("accreditation_statuses", hasKey(accreditation.id().value().toString()));
    }

    @Test
    void leavesRedisOutOfTheReadinessCheck() {
        var checks = given().when().get("/q/health/ready").then().extract().jsonPath().getList("checks.name", String.class);

        assertThat(checks).isNotEmpty().noneMatch(name -> name.contains("Redis"));
    }

    private static Accreditation pending(String userId) {
        var now = ZonedDateTime.now();
        return Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(new UserId(userId))
                .type(BY_INCOME)
                .status(PENDING)
                .documentId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // The global test resources would start a Redis and a Kafka broker for this application too. Kafka points nowhere
    // as well, so the consumer can't join the group of the Compose stack.
    public static class RedisUnreachable implements QuarkusTestProfile {

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.redis.hosts", "redis://localhost:1",
                    "kafka.bootstrap.servers", "localhost:1");
        }
    }
}
