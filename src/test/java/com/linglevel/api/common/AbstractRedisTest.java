package com.linglevel.api.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis TestContainers를 사용하는 테스트의 추상 클래스
 *
 * 모든 테스트가 하나의 Redis 컨테이너를 공유하여 안정성과 성능을 보장합니다.
 *
 * 사용법:
 * @SpringBootTest
 * class MyRedisTest extends AbstractRedisTest {
 *     // TestContainers 설정 불필요, 상속받음
 * }
 */
public abstract class AbstractRedisTest {

    private static GenericContainer<?> redisContainer;

    static {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redisContainer.start();
    }

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    // 컨테이너 정리를 위한 메서드 (필요 시)
    public static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }
}
