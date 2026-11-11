package com.willowwealth.accreditation.adapter.messaging.deserializer;

import com.willowwealth.accreditation.adapter.messaging.message.AccreditationEventMessage;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import lombok.extern.slf4j.Slf4j;

// A message that is not valid JSON fails here, before the consumer sees it, and the connector moves it to the
// dead-letter topic. Without this log line that would only show up as an INFO message of the connector.
@Slf4j
public class AccreditationEventMessageDeserializer extends ObjectMapperDeserializer<AccreditationEventMessage> {

    public AccreditationEventMessageDeserializer() {
        super(AccreditationEventMessage.class);
    }

    @Override
    public AccreditationEventMessage deserialize(String topic, byte[] data) {
        try {
            return super.deserialize(topic, data);
        } catch (RuntimeException e) {
            log.error("Could not read a message from {}, moving it to the dead-letter topic: {}", topic, e.getMessage());
            throw e;
        }
    }
}
