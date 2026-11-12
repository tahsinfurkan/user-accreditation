package com.willowwealth.accreditation.adapter.in.scheduler;

import com.willowwealth.accreditation.application.port.in.command.DeleteOrphanedDocumentsCommand;
import com.willowwealth.accreditation.application.port.in.usecase.DeleteOrphanedDocumentsUseCase;
import com.willowwealth.accreditation.config.DocumentCleanupConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OrphanedDocumentCleanupJob {

    private static final String RUN_ID = "run_id";

    private final DeleteOrphanedDocumentsUseCase deleteOrphanedDocuments;
    private final DocumentCleanupConfig config;
    private final MeterRegistry registry;

    // Deleting a document twice is harmless, so overlapping runs on several instances do no damage.
    @Scheduled(every = "${accreditation.document-cleanup.interval}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void deleteOrphanedDocuments() {
        MDC.put(RUN_ID, UUID.randomUUID().toString());
        var started = System.nanoTime();
        try {
            var deleted = deleteOrphanedDocuments.delete(new DeleteOrphanedDocumentsCommand(config.gracePeriod()));
            registry.counter("accreditation.orphaned.documents.deleted").increment(deleted);
            log.info("Document cleanup finished in {} ms, documents deleted: {}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), deleted);
        } catch (RuntimeException e) {
            log.error("Document cleanup failed, the next run will try again", e);
        } finally {
            MDC.remove(RUN_ID);
        }
    }
}
