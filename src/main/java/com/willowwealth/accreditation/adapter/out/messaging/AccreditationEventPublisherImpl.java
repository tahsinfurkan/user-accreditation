package com.willowwealth.accreditation.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willowwealth.accreditation.adapter.messaging.mapper.AccreditationEventMessageMapper;
import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.config.MessagingConfig;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.willowwealth.accreditation.config.CircuitBreakerProducer.KAFKA;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;

// A producer of its own instead of a SmallRye emitter: the emitter sends every record from one thread, so during an
// outage records wait behind each other and could still be sent after the request that published them had been
// answered with 503. Here each request sends on its own thread and waits for its own record.
@Slf4j
@ApplicationScoped
public class AccreditationEventPublisherImpl implements AccreditationEventPublisher {

    private static final String TRACE_ID = "trace_id";

    private final Producer<String, AccreditationEventMessage> producer;
    private final AccreditationEventMessageMapper mapper;
    private final String topic;
    private final Duration acknowledgementTimeout;

    AccreditationEventPublisherImpl(AccreditationEventMessageMapper mapper, ObjectMapper objectMapper,
                                    MessagingConfig config) {
        this.mapper = mapper;
        this.topic = config.topic();
        this.producer = new KafkaProducer<>(Map.of(
                BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers(),
                CLIENT_ID_CONFIG, "accreditation-event-publisher",
                ACKS_CONFIG, "all",
                ENABLE_IDEMPOTENCE_CONFIG, "true",
                MAX_BLOCK_MS_CONFIG, String.valueOf(config.maxBlock().toMillis()),
                REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(config.requestTimeout().toMillis()),
                DELIVERY_TIMEOUT_MS_CONFIG, String.valueOf(config.deliveryTimeout().toMillis())),
                new StringSerializer(), new ObjectMapperSerializer<>(objectMapper));
        // Once send() has returned, the producer settles the record within delivery.timeout.ms. Waiting a little
        // longer means the answer is always the producer's own; the extra second only guards against one that hangs.
        this.acknowledgementTimeout = config.deliveryTimeout().plusSeconds(1);
    }

    @Override
    public void publish(AccreditationEvent event) {
        var message = mapper.toMessage(event);
        try {
            send(message);
        } catch (CircuitBreakerOpenException e) {
            // For the application this is the same as Kafka not answering, only quicker.
            throw new EventPublicationException(
                    "Event %s was not sent, the Kafka circuit breaker is open".formatted(message.eventId()), e);
        }
        log.debug("Published {} {} for accreditation {}", message.eventType(), message.eventId(), message.accreditationId());
    }

    // Quarkus intercepts calls a bean makes to its own methods, so the circuit breaker applies to this call.
    @ApplyGuard(KAFKA)
    void send(AccreditationEventMessage message) {
        try {
            // Keyed by user, so all events of one user land on the same partition and stay in order. send() itself
            // blocks for at most max.block.ms while the producer has no metadata for the topic.
            producer.send(record(message)).get(acknowledgementTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw notAcknowledged(message, e);
        } catch (ExecutionException e) {
            throw notAcknowledged(message, e.getCause());
        } catch (TimeoutException | RuntimeException e) {
            throw notAcknowledged(message, e);
        }
    }

    // The trace id of the request travels as a header, so the consumer's log lines carry it too. Events of the expiry
    // job have none.
    private ProducerRecord<String, AccreditationEventMessage> record(AccreditationEventMessage message) {
        var record = new ProducerRecord<>(topic, message.userId(), message);
        var traceId = MDC.get(TRACE_ID);
        if (traceId != null) {
            record.headers().add(TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @PreDestroy
    void close() {
        producer.close(Duration.ofSeconds(5));
    }

    private static EventPublicationException notAcknowledged(AccreditationEventMessage message, Throwable cause) {
        return new EventPublicationException("Kafka did not acknowledge event %s".formatted(message.eventId()), cause);
    }
}
