package com.willowwealth.accreditation.domain.model;

import java.util.regex.Pattern;

public record UserId(String value) {

    // Public so that request validation can reuse the same rule.
    public static final String PATTERN = "[A-Za-z0-9]{32}";

    private static final Pattern FORMAT = Pattern.compile(PATTERN);

    public UserId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("user id must be exactly 32 letters or digits");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
