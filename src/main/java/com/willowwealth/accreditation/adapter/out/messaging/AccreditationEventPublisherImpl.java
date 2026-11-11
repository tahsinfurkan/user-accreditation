package com.willowwealth.accreditation.adapter.out.messaging;

import com.willowwealth.accreditation.adapter.messaging.mapper.AccreditationEventMessageMapper;
import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.time.Duration;

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
            // Keyed by user, so all events of one user land on the same partition and stay in order.
            emitter.send(Record.of(message.userId(), message)).await().atMost(sendTimeout);
        } catch (RuntimeException e) {
            throw new EventPublicationException(
                    "Event %s was not acknowledged by Kafka within %s".formatted(message.eventId(), sendTimeout), e);
        }
        log.debug("Published {} {} for accreditation {}", message.eventType(), message.eventId(), message.accreditationId());
    }
}
