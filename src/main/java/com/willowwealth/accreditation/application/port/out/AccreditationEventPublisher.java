package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;

public interface AccreditationEventPublisher {

    /**
     * Returns only after the broker has acknowledged the event.
     *
     * @throws EventPublicationException if the event was not acknowledged in time, or not sent at all because Kafka
     *                                   kept failing
     */
    void publish(AccreditationEvent event);
}
