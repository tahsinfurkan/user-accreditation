package com.willowwealth.accreditation.application.port.out;

import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.domain.event.AccreditationEvent;

public interface AccreditationEventPublisher {

    /**
     * Returns only after the broker has acknowledged the event.
     *
     * @throws EventPublicationException if Kafka did not take the event; the publisher has stopped trying by then,
     *                                   so it does not send the event later
     */
    void publish(AccreditationEvent event);
}
