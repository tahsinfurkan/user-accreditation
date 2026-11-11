package com.willowwealth.accreditation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void acceptsThirtyTwoLettersAndDigits() {
        assertThat(new UserId("g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V").value()).hasSize(32);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1",   // 31 characters
            "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1VX", // 33 characters
            "g8NlYJnk7zK9BlB1-2Ebjs0AkhCTpE1V",
            "g8NlYJnk7zK9BlB1 2Ebjs0AkhCTpE1V",
            "g8NlYJnk7zK9BlB1ğ2Ebjs0AkhCTpE1V"
    })
    void rejectsAnythingElse(String value) {
        assertThatThrownBy(() -> new UserId(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
