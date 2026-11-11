package com.willowwealth.accreditation.adapter.out.persistence;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

// application.properties points at the Redis from Docker Compose, which also keeps Dev Services from
// starting one. Tests get their own container instead.
public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private static final int REDIS_PORT = 6379;

    private final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:8.8.4-alpine")).withExposedPorts(REDIS_PORT);

    @Override
    public Map<String, String> start() {
        redis.start();
        return Map.of("quarkus.redis.hosts", "redis://%s:%d".formatted(redis.getHost(), redis.getMappedPort(REDIS_PORT)));
    }

    @Override
    public void stop() {
        redis.stop();
    }
}
