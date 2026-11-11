package com.willowwealth.accreditation.domain.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

// Points to a stored document. Events carry this instead of the file itself.
public record DocumentReference(UUID id, String name, String mimeType, long sizeBytes, String sha256) {

    public DocumentReference {
        Objects.requireNonNull(id, "document id is required");
    }

    public static DocumentReference of(UUID id, Document document) {
        return new DocumentReference(id, document.name(), document.mimeType(), document.content().length,
                sha256(document.content()));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
