package com.willowwealth.accreditation.adapter.in.scheduler;

import com.willowwealth.accreditation.application.port.in.command.ExpireConfirmedAccreditationsCommand;
import com.willowwealth.accreditation.application.port.in.usecase.ExpireConfirmedAccreditationsUseCase;
import com.willowwealth.accreditation.config.AccreditationExpiryConfig;
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

// The spec only asks for CONFIRMED accreditations to expire (via the API or the
// 30-day job). Expiring stale PENDING submissions after a few days would make
// sense too, but it's left out on purpose to stick to the spec.
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class AccreditationExpiryJob {

    private static final String RUN_ID = "run_id";

    private final ExpireConfirmedAccreditationsUseCase expireConfirmedAccreditations;
    private final AccreditationExpiryConfig config;
    private final MeterRegistry registry;

    // The first run starts with the application, so whatever expired while it was down doesn't wait another interval.
    // SKIP only stops overlapping runs within one instance; across instances the consumer's CAS makes duplicates
    // harmless.
    @Scheduled(every = "${accreditation.expiry.interval}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void expireConfirmedAccreditations() {
        MDC.put(RUN_ID, UUID.randomUUID().toString());
        var started = System.nanoTime();
        try {
            var expiries = expireConfirmedAccreditations.expire(
                    new ExpireConfirmedAccreditationsCommand(config.confirmedTtl(), config.batchSize()));
            registry.counter("accreditation.expiries").increment(expiries);
            log.info("Expiry run finished in {} ms, expiries published: {}", millisSince(started), expiries);
        } catch (RuntimeException e) {
            // Usually the database could not be read. Whatever is still CONFIRMED is picked up by the next run.
            log.error("Expiry run failed after {} ms", millisSince(started), e);
        } finally {
            MDC.remove(RUN_ID);
        }
    }

    private static long millisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
