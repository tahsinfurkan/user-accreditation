package com.willowwealth.accreditation.config;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;

// One circuit breaker per dependency. Every method guarded with the same name shares its state, so once the
// database is down all database calls fail fast, not only the ones that happened to fail first.
@Slf4j
public class CircuitBreakerProducer {

    public static final String KAFKA = "kafka";
    public static final String DATABASE = "database";

    @Produces
    @Singleton
    @Identifier(KAFKA)
    Guard kafka(CircuitBreakerConfig config) {
        return circuitBreaker(KAFKA, config);
    }

    @Produces
    @Singleton
    @Identifier(DATABASE)
    Guard database(CircuitBreakerConfig config) {
        return circuitBreaker(DATABASE, config);
    }

    private static Guard circuitBreaker(String name, CircuitBreakerConfig config) {
        // The description names the circuit in exception messages, e.g. "database circuit breaker is open".
        return Guard.create()
                .withDescription(name)
                .withCircuitBreaker()
                .name(name)
                .requestVolumeThreshold(config.requestVolumeThreshold())
                .failureRatio(config.failureRatio())
                .delay(config.delay().toMillis(), ChronoUnit.MILLIS)
                .onStateChange(state -> {
                    if (state == CircuitBreakerState.OPEN) {
                        log.warn("The {} circuit breaker opened, calls fail fast for the next {} s", name,
                                config.delay().toSeconds());
                    } else {
                        log.info("The {} circuit breaker is {}", name, state);
                    }
                })
                .done()
                .build();
    }
}
