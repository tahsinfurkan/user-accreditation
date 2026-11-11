package com.willowwealth.accreditation.config;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import java.time.temporal.ChronoUnit;

import static io.smallrye.faulttolerance.api.RateLimitType.ROLLING;

// Every endpoint gets a rate limiter of its own, so a burst on one of them never turns requests to another away.
// The window is rolling: no window-long span lets more requests through than the limit.
public class RateLimitProducer {

    public static final String SUBMIT_RATE_LIMIT = "submit-rate-limit";
    public static final String FINALIZE_RATE_LIMIT = "finalize-rate-limit";
    public static final String HISTORY_RATE_LIMIT = "history-rate-limit";
    public static final String USER_ACCREDITATIONS_RATE_LIMIT = "user-accreditations-rate-limit";

    @Produces
    @Singleton
    @Identifier(SUBMIT_RATE_LIMIT)
    Guard submit(RateLimitConfig config) {
        return rateLimit(SUBMIT_RATE_LIMIT, config.admin(), config);
    }

    @Produces
    @Singleton
    @Identifier(FINALIZE_RATE_LIMIT)
    Guard finalizeAccreditation(RateLimitConfig config) {
        return rateLimit(FINALIZE_RATE_LIMIT, config.admin(), config);
    }

    @Produces
    @Singleton
    @Identifier(HISTORY_RATE_LIMIT)
    Guard history(RateLimitConfig config) {
        return rateLimit(HISTORY_RATE_LIMIT, config.admin(), config);
    }

    @Produces
    @Singleton
    @Identifier(USER_ACCREDITATIONS_RATE_LIMIT)
    Guard userAccreditations(RateLimitConfig config) {
        return rateLimit(USER_ACCREDITATIONS_RATE_LIMIT, config.client(), config);
    }

    private static Guard rateLimit(String name, int limit, RateLimitConfig config) {
        return Guard.create()
                .withDescription(name)
                .withRateLimit()
                .limit(limit)
                .window(config.window().toMillis(), ChronoUnit.MILLIS)
                .type(ROLLING)
                .done()
                .build();
    }
}
