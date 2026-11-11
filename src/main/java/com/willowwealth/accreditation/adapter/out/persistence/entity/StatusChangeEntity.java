package com.willowwealth.accreditation.adapter.out.persistence.entity;

import com.willowwealth.accreditation.domain.event.ChangeOrigin;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "accreditation_status_history")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class StatusChangeEntity {

    @Id
    @Column(name = "event_id")
    @ToString.Include
    private UUID eventId;

    @Column(name = "accreditation_id", nullable = false)
    @ToString.Include
    private UUID accreditationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private AccreditationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private AccreditationStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeOrigin origin;

    @Column(name = "occurred_at", nullable = false)
    private ZonedDateTime occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private ZonedDateTime recordedAt;
}
