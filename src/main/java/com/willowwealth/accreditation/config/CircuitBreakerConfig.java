package com.willowwealth.accreditation.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;

@ConfigMapping(prefix = "accreditation.circuit-breaker")
public interface CircuitBreakerConfig {

    // A circuit opens when at least this share of the last request-volume-threshold calls failed...
    int requestVolumeThreshold();

    double failureRatio();

    // ...and stays open this long before it lets a trial call through.
    Duration delay();
}
