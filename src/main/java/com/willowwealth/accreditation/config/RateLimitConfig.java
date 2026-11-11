package com.willowwealth.accreditation.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;

@ConfigMapping(prefix = "accreditation.rate-limit")
public interface RateLimitConfig {

    // How many requests each admin endpoint accepts within one window...
    int admin();

    // ...and the client endpoint, which mostly reads from the Redis cache.
    int client();

    Duration window();
}
