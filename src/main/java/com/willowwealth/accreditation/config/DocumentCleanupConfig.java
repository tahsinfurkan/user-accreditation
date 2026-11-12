package com.willowwealth.accreditation.config;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;

@ConfigMapping(prefix = "accreditation.document-cleanup")
public interface DocumentCleanupConfig {

    // Only @Scheduled reads it, straight from the property.
    String interval();

    Duration gracePeriod();
}
