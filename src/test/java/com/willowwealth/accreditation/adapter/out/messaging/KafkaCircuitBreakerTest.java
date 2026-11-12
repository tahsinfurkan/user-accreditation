package com.willowwealth.accreditation.adapter.out.messaging;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.config.CircuitBreakerProducer.KAFKA;
import static io.restassured.RestAssured.given;
import static io.smallrye.faulttolerance.api.CircuitBreakerState.OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// A submission that waited for Kafka in vain is answered with Retry-After 5; one the open circuit turned away
// with the circuit's delay, which is 2 seconds in tests.
@QuarkusTest
@TestProfile(KafkaCircuitBreakerTest.KafkaUnreachable.class)
class KafkaCircuitBreakerTest {

    @Inject
    CircuitBreakerMaintenance circuitBreakers;

    @BeforeEach
    void closeTheCircuit() {
        circuitBreakers.resetAll();
    }

    @Test
    void answersOnlyOnceTheProducerHasGivenUpOnTheEvent() {
        var started = System.nanoTime();

        submit().statusCode(503).header("Retry-After", "5");

        // The send timeout is 10 seconds. An answer this early is the producer's own failure, so it has stopped trying
        // and won't write the event after the client was told it failed.
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void turnsSubmissionsAwayOnceKafkaKeepsFailingAndTriesAgainAfterTheDelay() {
        submit().statusCode(503).header("Retry-After", "5");

        await().atMost(Duration.ofSeconds(10)).until(() -> "2".equals(submit().extract().header("Retry-After")));
        assertThat(circuitBreakers.currentState(KAFKA)).isEqualTo(OPEN);
        assertErrorBody(submit(), 503, "Service temporarily unavailable");

        // Once the delay is over, a submission goes to Kafka again as a trial. It fails, so the circuit opens again.
        await().atMost(Duration.ofSeconds(10)).until(() -> "5".equals(submit().extract().header("Retry-After")));
        assertThat(circuitBreakers.currentState(KAFKA)).isEqualTo(OPEN);
    }

    private static ValidatableResponse submit() {
        return given().contentType(ContentType.JSON).body("""
                        {"user_id": "%s", "accreditation_type": "BY_INCOME",
                         "document": {"name": "2021.pdf", "mime_type": "application/pdf", "content": "JVBERi0xLjcK"}}"""
                        .formatted(newUserId()))
                .when().post("/api/v1/user/accreditation")
                .then();
    }

    // Only the publisher's channel points to a port nobody listens on; everything else uses the test broker.
    // Short producer limits make each submission fail within a fraction of a second.
    public static class KafkaUnreachable implements QuarkusTestProfile {

        private static final String PRODUCER = "mp.messaging.outgoing.accreditation-events-out.";

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    PRODUCER + "bootstrap.servers", "localhost:1",
                    PRODUCER + "max.block.ms", "200",
                    PRODUCER + "request.timeout.ms", "200",
                    PRODUCER + "delivery.timeout.ms", "300");
        }
    }
}
