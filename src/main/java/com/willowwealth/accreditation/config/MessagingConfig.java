package com.willowwealth.accreditation.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

@ConfigMapping(prefix = "accreditation.messaging")
public interface MessagingConfig {

    String topic();

    // The broker everything else uses; tests point the publisher alone at an address nobody listens on.
    @WithDefault("${kafka.bootstrap.servers}")
    String bootstrapServers();

    // How long publishing may block while the producer has no metadata for the topic...
    Duration maxBlock();

    // ...how long it waits for the broker to answer a request...
    Duration requestTimeout();

    // ...and how long it keeps trying to deliver an event before it gives up on it.
    Duration deliveryTimeout();
}
