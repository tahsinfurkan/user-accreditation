package com.willowwealth.accreditation.adapter.in;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

// Tests share the application and its counters, so they compare a counter before and after what they do.
public final class Counters {

    private Counters() {
    }

    public static double count(MeterRegistry registry, String name, String... tags) {
        return registry.find(name).tags(tags).counters().stream().mapToDouble(Counter::count).sum();
    }
}
