package com.willowwealth.accreditation.adapter.out.messaging;

import com.willowwealth.accreditation.adapter.messaging.mapper.AccreditationEventMessageMapper;
import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.time.Duration;

import static com.willowwealth.accreditation.config.CircuitBreakerProducer.KAFKA;

@Slf4j
@ApplicationScoped
public class AccreditationEventPublisherImpl implements AccreditationEventPublisher {

    private final MutinyEmitter<Record<String, AccreditationEventMessage>> emitter;
    private final AccreditationEventMessageMapper mapper;
    private final Duration sendTimeout;

    AccreditationEventPublisherImpl(
            @Channel("accreditation-events-out") MutinyEmitter<Record<String, AccreditationEventMessage>> emitter,
            AccreditationEventMessageMapper mapper,
            @ConfigProperty(name = "accreditation.messaging.send-timeout") Duration sendTimeout) {
        this.emitter = emitter;
        this.mapper = mapper;
        this.sendTimeout = sendTimeout;
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
            // Keyed by user, so all events of one user land on the same partition and stay in order.
            // The producer gives up on a record by itself: send() blocks for at most max.block.ms, and the connector
            // retries only until delivery.timeout.ms has passed, each try taking up to delivery.timeout.ms again.
            // The send timeout is longer than all of that, so a failure here is the producer's own and it won't
            // send the record after the caller has been told it failed.
            emitter.send(Record.of(message.userId(), message)).await().atMost(sendTimeout);
        } catch (RuntimeException e) {
            throw new EventPublicationException("Kafka did not acknowledge event %s".formatted(message.eventId()), e);
        }
    }
}
