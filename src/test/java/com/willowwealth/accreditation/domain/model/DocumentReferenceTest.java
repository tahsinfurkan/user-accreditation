package com.willowwealth.accreditation.domain.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentReferenceTest {

    @Test
    void describesTheDocumentBySizeAndSha256() {
        var document = new Document("hello.pdf", "application/pdf", "hello".getBytes(StandardCharsets.UTF_8));

        var reference = DocumentReference.of(UUID.randomUUID(), document);

        assertThat(reference.sizeBytes()).isEqualTo(5);
        assertThat(reference.sha256()).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(reference.name()).isEqualTo("hello.pdf");
    }
}
