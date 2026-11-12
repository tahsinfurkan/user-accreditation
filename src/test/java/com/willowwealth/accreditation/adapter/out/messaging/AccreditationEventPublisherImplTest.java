package com.willowwealth.accreditation.adapter.out.messaging;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.submit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class AccreditationEventPublisherImplTest {

    @Inject
    MeterRegistry registry;

    @Test
    void countsTheRecordsItSends() {
        submit(newUserId(), "BY_INCOME");

        // The Kafka client updates its own counters a moment after the acknowledgement.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(registry
                .find("kafka.producer.record.send.total").tag("client.id", "accreditation-event-publisher")
                .functionCounters()).extracting(FunctionCounter::count).anySatisfy(sent -> assertThat(sent).isPositive()));
    }
}
