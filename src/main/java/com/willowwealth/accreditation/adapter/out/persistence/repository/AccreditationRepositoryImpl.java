package com.willowwealth.accreditation.adapter.out.persistence.repository;

import com.willowwealth.accreditation.adapter.out.persistence.entity.AccreditationEntity;
import com.willowwealth.accreditation.adapter.out.persistence.mapper.AccreditationEntityMapper;
import com.willowwealth.accreditation.adapter.out.persistence.projection.AccreditationSummaryRow;
import com.willowwealth.accreditation.adapter.out.persistence.projection.AccreditationSummaryRows;
import com.willowwealth.accreditation.application.port.out.AccreditationRepository;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationStatus;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.willowwealth.accreditation.config.CircuitBreakerProducer.DATABASE;

@Slf4j
@ApplicationScoped
public class AccreditationRepositoryImpl
        implements AccreditationRepository, PanacheRepositoryBase<AccreditationEntity, UUID> {

    static final String USER_ACCREDITATIONS_CACHE = "user-accreditations";
    private static final Duration EVICTION_TIMEOUT = Duration.ofSeconds(1);

    private final AccreditationEntityMapper mapper;
    private final Cache userAccreditations;
    private final TransactionSynchronizationRegistry transactions;

    AccreditationRepositoryImpl(AccreditationEntityMapper mapper,
                                @CacheName(USER_ACCREDITATIONS_CACHE) Cache userAccreditations,
                                TransactionSynchronizationRegistry transactions) {
        this.mapper = mapper;
        this.userAccreditations = userAccreditations;
        this.transactions = transactions;
    }

    @Override
    @ApplyGuard(DATABASE)
    public Optional<Accreditation> findById(AccreditationId id) {
        return findByIdOptional(id.value()).map(mapper::toDomain);
    }

    @Override
    @ApplyGuard(DATABASE)
    public void insert(Accreditation accreditation) {
        persist(mapper.toEntity(accreditation));
        evictAfterCommit(accreditation.userId());
    }

    @Override
    @ApplyGuard(DATABASE)
    public void update(Accreditation accreditation) {
        var entity = findByIdOptional(accreditation.id().value()).orElseThrow();
        mapper.updateEntity(accreditation, entity);
        evictAfterCommit(accreditation.userId());
    }

    // Read from the database on purpose: this check has to see the latest state, not a cached one.
    @Override
    @ApplyGuard(DATABASE)
    public boolean existsPendingFor(UserId userId) {
        return count("userId = ?1 and status = ?2", userId.value(), AccreditationStatus.PENDING) > 0;
    }

    // Keyset pagination: the consumer may expire rows while the job is still paging,
    // which would shift an offset and make it skip rows.
    @Override
    @ApplyGuard(DATABASE)
    public List<Accreditation> findConfirmedUpdatedBefore(ZonedDateTime cutoff, Accreditation after, int limit) {
        var order = Sort.by("updatedAt").and("id");
        var query = after == null
                ? find("status = ?1 and updatedAt < ?2", order, AccreditationStatus.CONFIRMED, cutoff)
                : find("status = ?1 and updatedAt < ?2 and (updatedAt > ?3 or (updatedAt = ?3 and id > ?4))", order,
                        AccreditationStatus.CONFIRMED, cutoff, after.updatedAt(), after.id().value());
        return query.range(0, limit - 1).list().stream().map(mapper::toDomain).toList();
    }

    // Redis is only a shortcut: when it can't be reached, the rows come straight from the database. A failure of the
    // database itself is passed on, since asking it a second time wouldn't help.
    @Override
    public List<AccreditationSummary> findSummariesByUserId(UserId userId) {
        AccreditationSummaryRows rows;
        try {
            rows = cachedSummaryRows(userId.value());
        } catch (PersistenceException | CircuitBreakerOpenException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("The cache is unavailable, reading the accreditations of user {} from the database: {}", userId,
                    e.toString());
            rows = summaryRows(userId.value());
        }
        return mapper.toSummaries(rows.rows());
    }

    // Cached in Redis, so every instance shares the entry. Quarkus also intercepts calls a bean makes to its
    // own methods, which is why the cache applies to this internal call. A cached entry is served even while the
    // database circuit is open.
    @CacheResult(cacheName = USER_ACCREDITATIONS_CACHE)
    AccreditationSummaryRows cachedSummaryRows(String userId) {
        return summaryRows(userId);
    }

    @ApplyGuard(DATABASE)
    AccreditationSummaryRows summaryRows(String userId) {
        log.debug("Loading the accreditations of user {} from the database", userId);
        return new AccreditationSummaryRows(find("userId", Sort.by("createdAt"), userId)
                .project(AccreditationSummaryRow.class)
                .list());
    }

    // Evicting before the commit would let a concurrent read put the old rows straight back into the cache.
    private void evictAfterCommit(UserId userId) {
        transactions.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status != Status.STATUS_COMMITTED) {
                    return;
                }
                // The write is committed either way; a stale entry only lives until it expires.
                try {
                    userAccreditations.invalidate(userId.value()).await().atMost(EVICTION_TIMEOUT);
                    log.debug("Evicted the cached accreditations of user {}", userId);
                } catch (RuntimeException e) {
                    log.warn("Could not evict the cached accreditations of user {}, they stay until they expire: {}",
                            userId, e.toString());
                }
            }
        });
    }
}
