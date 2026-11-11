package com.willowwealth.accreditation.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;

@ConfigMapping(prefix = "accreditation.expiry")
public interface AccreditationExpiryConfig {

    // Only @Scheduled reads it, straight from the property. A string, so that "off" can switch the job off.
    String interval();

    Duration confirmedTtl();

    int batchSize();
}
