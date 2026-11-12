package com.willowwealth.accreditation.adapter.in.scheduler;

import com.willowwealth.accreditation.adapter.out.persistence.mapper.DocumentEntityMapper;
import com.willowwealth.accreditation.adapter.out.persistence.repository.AccreditationRepositoryImpl;
import com.willowwealth.accreditation.adapter.out.persistence.repository.DocumentRepositoryImpl;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.Document;
import com.willowwealth.accreditation.domain.model.DocumentReference;
import com.willowwealth.accreditation.domain.model.UserId;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.willowwealth.accreditation.adapter.in.Counters.count;
import static com.willowwealth.accreditation.adapter.in.resource.AccreditationRequests.newUserId;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;

// The scheduler is off in tests, so the test runs the job itself.
@QuarkusTest
class OrphanedDocumentCleanupJobTest {

    private static final String DELETED = "accreditation.orphaned.documents.deleted";

    @Inject
    OrphanedDocumentCleanupJob job;

    @Inject
    DocumentRepositoryImpl documents;

    @Inject
    DocumentEntityMapper documentMapper;

    @Inject
    AccreditationRepositoryImpl accreditations;

    @Inject
    MeterRegistry registry;

    @Test
    void deletesDocumentsNoAccreditationRefersToOnceTheyAreADayOld() {
        var twoDaysAgo = ZonedDateTime.now().minusDays(2);
        var orphan = store(twoDaysAgo);
        var recentOrphan = store(ZonedDateTime.now());
        var referenced = store(twoDaysAgo);
        QuarkusTransaction.requiringNew().run(() -> accreditations.insert(accreditationWith(referenced)));
        var deleted = count(registry, DELETED);

        job.deleteOrphanedDocuments();

        assertThat(exists(orphan)).isFalse();
        // Its submission may still be waiting in Kafka.
        assertThat(exists(recentOrphan)).isTrue();
        assertThat(exists(referenced)).isTrue();
        // Old orphans left by other tests can go in the same run.
        assertThat(count(registry, DELETED)).isGreaterThanOrEqualTo(deleted + 1);
    }

    private UUID store(ZonedDateTime createdAt) {
        var content = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);
        var reference = DocumentReference.of(UUID.randomUUID(), new Document("2021.pdf", "application/pdf", content));
        QuarkusTransaction.requiringNew()
                .run(() -> documents.persist(documentMapper.toEntity(reference, content, createdAt)));
        return reference.id();
    }

    private boolean exists(UUID documentId) {
        return QuarkusTransaction.requiringNew().call(() -> documents.findByIdOptional(documentId).isPresent());
    }

    private static Accreditation accreditationWith(UUID documentId) {
        var now = ZonedDateTime.now();
        return Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(new UserId(newUserId()))
                .type(BY_INCOME)
                .status(PENDING)
                .documentId(documentId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
