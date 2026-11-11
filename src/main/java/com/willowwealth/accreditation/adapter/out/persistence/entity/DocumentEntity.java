package com.willowwealth.accreditation.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accreditation_document")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class DocumentEntity {

    @Id
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    // Left out of toString: up to 5 MB of a user's personal financial document.
    @Column(nullable = false)
    private byte[] content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
