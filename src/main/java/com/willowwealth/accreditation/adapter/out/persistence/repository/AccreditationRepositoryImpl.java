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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class AccreditationRepositoryImpl
        implements AccreditationRepository, PanacheRepositoryBase<AccreditationEntity, UUID> {

    static final String USER_ACCREDITATIONS_CACHE = "user-accreditations";

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
    public boolean exists(AccreditationId id) {
        return count("id", id.value()) > 0;
    }

    @Override
    public void insert(Accreditation accreditation) {
        persist(mapper.toEntity(accreditation));
        evictAfterCommit(accreditation.userId());
    }

    // Read from the database on purpose: this check has to see the latest state, not a cached one.
    @Override
    public boolean existsPendingFor(UserId userId) {
        return count("userId = ?1 and status = ?2", userId.value(), AccreditationStatus.PENDING) > 0;
    }

    @Override
    public List<AccreditationSummary> findSummariesByUserId(UserId userId) {
        return mapper.toSummaries(summaryRows(userId.value()).rows());
    }

    // Cached in Redis, so every instance shares the entry. Quarkus also intercepts calls a bean makes to its
    // own methods, which is why the cache applies to this internal call.
    @CacheResult(cacheName = USER_ACCREDITATIONS_CACHE)
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
                if (status == Status.STATUS_COMMITTED) {
                    userAccreditations.invalidate(userId.value()).await().indefinitely();
                    log.debug("Evicted the cached accreditations of user {}", userId);
                }
            }
        });
    }
}
