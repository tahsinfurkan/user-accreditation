package com.willowwealth.accreditation.adapter.out.persistence.repository;

import com.willowwealth.accreditation.adapter.out.persistence.RedisTestResource;
import com.willowwealth.accreditation.domain.model.Accreditation;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import com.willowwealth.accreditation.domain.model.AccreditationSummary;
import com.willowwealth.accreditation.domain.model.UserId;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.willowwealth.accreditation.domain.model.AccreditationStatus.PENDING;
import static com.willowwealth.accreditation.domain.model.AccreditationType.BY_INCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class AccreditationRepositoryImplTest {

    @Inject
    AccreditationRepositoryImpl repository;

    @Inject
    RedisDataSource redis;

    @Test
    void servesTheSummariesFromRedisUntilTheUsersAccreditationsChange() {
        var userId = new UserId(UUID.randomUUID().toString().replace("-", ""));
        var first = pendingAccreditation(userId);
        QuarkusTransaction.requiringNew().run(() -> repository.insert(first));

        assertThat(idsOf(summariesOf(userId))).containsExactly(first.id());
        assertThat(redis.key().exists("cache:user-accreditations:" + userId.value())).isTrue();

        // Deleting the row behind the cache's back: the next read still sees it, so it came from Redis.
        QuarkusTransaction.requiringNew().run(() -> repository.deleteById(first.id().value()));
        assertThat(idsOf(summariesOf(userId))).containsExactly(first.id());

        var second = pendingAccreditation(userId);
        QuarkusTransaction.requiringNew().run(() -> repository.insert(second));

        assertThat(idsOf(summariesOf(userId))).containsExactly(second.id());
    }

    @Test
    void keepsTheCachedSummariesWhenTheInsertIsRolledBack() {
        var userId = new UserId(UUID.randomUUID().toString().replace("-", ""));
        var first = pendingAccreditation(userId);
        QuarkusTransaction.requiringNew().run(() -> repository.insert(first));
        summariesOf(userId);

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            repository.insert(pendingAccreditation(userId));
            throw new IllegalStateException("processing failed after the insert");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(redis.key().exists("cache:user-accreditations:" + userId.value())).isTrue();
        assertThat(idsOf(summariesOf(userId))).containsExactly(first.id());
    }

    @Test
    void knowsWhetherAUserHasAPendingAccreditation() {
        var userId = new UserId(UUID.randomUUID().toString().replace("-", ""));
        assertThat(hasPending(userId)).isFalse();

        QuarkusTransaction.requiringNew().run(() -> repository.insert(pendingAccreditation(userId)));

        assertThat(hasPending(userId)).isTrue();
    }

    @Test
    void refusesASecondPendingAccreditationForTheSameUser() {
        var userId = new UserId(UUID.randomUUID().toString().replace("-", ""));
        QuarkusTransaction.requiringNew().run(() -> repository.insert(pendingAccreditation(userId)));

        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> repository.insert(pendingAccreditation(userId))))
                .hasStackTraceContaining("ux_accreditation_pending_user");
    }

    private boolean hasPending(UserId userId) {
        return QuarkusTransaction.requiringNew().call(() -> repository.existsPendingFor(userId));
    }

    private List<AccreditationSummary> summariesOf(UserId userId) {
        return QuarkusTransaction.requiringNew().call(() -> repository.findSummariesByUserId(userId));
    }

    private static List<AccreditationId> idsOf(List<AccreditationSummary> summaries) {
        return summaries.stream().map(AccreditationSummary::id).toList();
    }

    private static Accreditation pendingAccreditation(UserId userId) {
        var now = Instant.now();
        return Accreditation.builder()
                .id(AccreditationId.newId())
                .userId(userId)
                .type(BY_INCOME)
                .status(PENDING)
                .documentId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
