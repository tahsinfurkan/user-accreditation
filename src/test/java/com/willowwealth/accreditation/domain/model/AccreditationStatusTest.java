package com.willowwealth.accreditation.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AccreditationStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "PENDING,   PENDING,   false",
            "PENDING,   CONFIRMED, true",
            "PENDING,   EXPIRED,   false",
            "PENDING,   FAILED,    true",
            "CONFIRMED, PENDING,   false",
            "CONFIRMED, CONFIRMED, false",
            "CONFIRMED, EXPIRED,   true",
            "CONFIRMED, FAILED,    false",
            "EXPIRED,   PENDING,   false",
            "EXPIRED,   CONFIRMED, false",
            "EXPIRED,   EXPIRED,   false",
            "EXPIRED,   FAILED,    false",
            "FAILED,    PENDING,   false",
            "FAILED,    CONFIRMED, false",
            "FAILED,    EXPIRED,   false",
            "FAILED,    FAILED,    false"
    })
    void allowsOnlyTheTransitionsFromTheSpec(AccreditationStatus from, AccreditationStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
