package com.willowwealth.accreditation.domain.model;

import java.util.Objects;

public record Document(String name, String mimeType, byte[] content) {

    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;

    public Document {
        Objects.requireNonNull(name, "document name is required");
        Objects.requireNonNull(mimeType, "document mime type is required");
        Objects.requireNonNull(content, "document content is required");
        if (content.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("document is larger than " + MAX_SIZE_BYTES + " bytes");
        }
    }
}
