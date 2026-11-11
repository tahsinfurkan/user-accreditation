package com.willowwealth.accreditation.config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.ZoneId;

public class ClockProducer {

    // New timestamps are taken in this zone, so they carry the offset of that day, summer or winter time.
    @Produces
    @Singleton
    Clock clock(@ConfigProperty(name = "accreditation.time-zone") ZoneId zone) {
        return Clock.system(zone);
    }
}
