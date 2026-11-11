package com.willowwealth.accreditation.application.service;

import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.in.command.ExpireConfirmedAccreditationsCommand;
import com.willowwealth.accreditation.application.port.in.usecase.ExpireConfirmedAccreditationsUseCase;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.model.Accreditation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import static com.willowwealth.accreditation.domain.event.ChangeOrigin.SYSTEM_EXPIRY;
import static com.willowwealth.accreditation.domain.model.AccreditationStatus.EXPIRED;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ExpireConfirmedAccreditationsService implements ExpireConfirmedAccreditationsUseCase {

    private final AccreditationRepository repository;
    private final AccreditationEventPublisher eventPublisher;
    private final Clock clock;

    // Like the API, this only publishes the changes; the consumer checks each one again before writing it.
    @Override
    public int expire(ExpireConfirmedAccreditationsCommand command) {
        var now = ZonedDateTime.now(clock);
        var cutoff = now.minus(command.confirmedTtl());
        var published = 0;
        var page = confirmedPage(cutoff, null, command.batchSize());
        while (!page.isEmpty()) {
            for (var accreditation : page) {
                try {
                    eventPublisher.publish(accreditation.changeStatus(EXPIRED, SYSTEM_EXPIRY, now));
                    published++;
                    log.debug("Accreditation {} of user {} is expiring, last updated at {}", accreditation.id(),
                            accreditation.userId(), accreditation.updatedAt());
                } catch (EventPublicationException e) {
                    // The rest would fail the same way. They are still CONFIRMED, so the next run picks them up.
                    log.error("Kafka is unreachable, stopping the expiry run after {} expiries", published, e);
                    return published;
                } catch (RuntimeException e) {
                    log.error("Could not expire accreditation {}, the next run will try again", accreditation.id(), e);
                }
            }
            page = page.size() < command.batchSize()
                    ? List.of()
                    : confirmedPage(cutoff, page.getLast(), command.batchSize());
        }
        return published;
    }

    // Every page is read in a short transaction of its own, so none stays open while the events are published.
    // Quarkus intercepts calls a bean makes to its own methods, which is why the annotation applies here.
    @Transactional
    List<Accreditation> confirmedPage(ZonedDateTime cutoff, Accreditation after, int size) {
        return repository.findConfirmedUpdatedBefore(cutoff, after, size);
    }
}
