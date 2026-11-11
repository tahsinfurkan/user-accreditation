package com.willowwealth.accreditation.adapter.out.persistence.entity;

import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "accreditation")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AccreditationEntity {

    @Id
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accreditation_type", nullable = false)
    private AccreditationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private AccreditationStatus status;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Version
    private long version;
}
