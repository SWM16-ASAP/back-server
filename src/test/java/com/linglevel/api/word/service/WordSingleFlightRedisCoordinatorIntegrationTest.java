package com.linglevel.api.word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglevel.api.common.AbstractRedisTest;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordAnalysisResult;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordSingleFlightRedisCoordinatorIntegrationTest extends AbstractRedisTest {

    private static final GenericContainer<?> redlockRedisA;
    private static final GenericContainer<?> redlockRedisB;
    private static final GenericContainer<?> redlockRedisC;

    static {
        redlockRedisA = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redlockRedisB = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redlockRedisC = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redlockRedisA.start();
        redlockRedisB.start();
        redlockRedisC.start();
    }

    private CoordinatorFixture nodeA;
    private CoordinatorFixture nodeB;

    @BeforeEach
    void setUp() {
        nodeA = createNode("test-model-a", 3_000);
        nodeB = createNode("test-model-a", 3_000);
        flushAll(nodeA.template);
    }

    @AfterEach
    void tearDown() {
        if (nodeA != null) {
            nodeA.close();
        }
        if (nodeB != null) {
            nodeB.close();
        }
    }

    @Test
    @DisplayName("실제 Redis에서 두 인스턴스 동시 요청 시 AI 호출은 1회만 수행된다")
    void deduplicatesAcrossTwoCoordinatorsUsingRealRedis() throws Exception {
        AtomicInteger aiCalls = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<List<WordAnalysisResult>> f1 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return nodeA.coordinator.execute("run", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    sleep(250);
                    return List.of(sample("run"));
                });
            });

            Future<List<WordAnalysisResult>> f2 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return nodeB.coordinator.execute("run", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    return List.of(sample("run"));
                });
            });

            start.countDown();

            List<WordAnalysisResult> r1 = f1.get(5, TimeUnit.SECONDS);
            List<WordAnalysisResult> r2 = f2.get(5, TimeUnit.SECONDS);

            assertThat(r1).hasSize(1);
            assertThat(r2).hasSize(1);
            assertThat(r1.get(0).getOriginalForm()).isEqualTo("run");
            assertThat(r2.get(0).getOriginalForm()).isEqualTo("run");
            assertThat(aiCalls.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("leader 실패는 실제 Redis resultKey를 통해 follower에도 동일 전파된다")
    void propagatesLeaderFailureAcrossTwoCoordinatorsUsingRealRedis() {
        RuntimeException leaderFailure = new RuntimeException("bedrock unavailable");
        AtomicInteger aiCalls = new AtomicInteger();

        assertThatThrownBy(() ->
                nodeA.coordinator.execute("left", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    throw leaderFailure;
                })
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("bedrock unavailable");

        assertThatThrownBy(() ->
                nodeB.coordinator.execute("left", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    return List.of(sample("left"));
                })
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Single-flight leader failed");

        assertThat(aiCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redlock(3노드) 구성에서도 두 인스턴스 동시 요청은 1회만 실행된다")
    void deduplicatesAcrossTwoCoordinatorsUsingRealRedisWithRedlock() throws Exception {
        CoordinatorFixture redlockNodeA = createNode("test-model-redlock", 3_000, true, redlockNodeAddresses());
        CoordinatorFixture redlockNodeB = createNode("test-model-redlock", 3_000, true, redlockNodeAddresses());
        flushAll(redlockNodeA.template);

        AtomicInteger aiCalls = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<List<WordAnalysisResult>> f1 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return redlockNodeA.coordinator.execute("sprint", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    sleep(250);
                    return List.of(sample("sprint"));
                });
            });

            Future<List<WordAnalysisResult>> f2 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return redlockNodeB.coordinator.execute("sprint", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    return List.of(sample("sprint"));
                });
            });

            start.countDown();

            List<WordAnalysisResult> r1 = f1.get(5, TimeUnit.SECONDS);
            List<WordAnalysisResult> r2 = f2.get(5, TimeUnit.SECONDS);

            assertThat(r1).hasSize(1);
            assertThat(r2).hasSize(1);
            assertThat(r1.get(0).getOriginalForm()).isEqualTo("sprint");
            assertThat(r2.get(0).getOriginalForm()).isEqualTo("sprint");
            assertThat(aiCalls.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            redlockNodeA.close();
            redlockNodeB.close();
        }
    }

    private CoordinatorFixture createNode(String model, long waitTimeoutMs) {
        return createNode(model, waitTimeoutMs, false, List.of());
    }

    private CoordinatorFixture createNode(String model, long waitTimeoutMs, boolean redlockEnabled, List<String> redlockNodeAddresses) {
        GenericContainer<?> redis = getRedisContainer();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));

        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        Config redissonConfig = new Config();
        redissonConfig.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
        RedissonClient redissonClient = Redisson.create(redissonConfig);

        WordSingleFlightProperties properties = new WordSingleFlightProperties();
        properties.setEnabled(true);
        properties.setLockTtlMs(5_000);
        properties.setWaitTimeoutMs(waitTimeoutMs);
        properties.setResultTtlMs(30_000);
        properties.setPromptVersion("v1");
        properties.setModel(model);
        properties.setSchemaVersion("v2");
        properties.setRedlockEnabled(redlockEnabled);
        properties.setRedlockNodeAddresses(redlockNodeAddresses);

        WordSingleFlightRedisCoordinator coordinator = new WordSingleFlightRedisCoordinator(
                template,
                listenerContainer,
                redissonClient,
                properties,
                new ObjectMapper()
        );
        ReflectionTestUtils.invokeMethod(coordinator, "initialize");

        return new CoordinatorFixture(connectionFactory, template, listenerContainer, redissonClient, coordinator);
    }

    private List<String> redlockNodeAddresses() {
        return List.of(
                toRedisAddress(redlockRedisA),
                toRedisAddress(redlockRedisB),
                toRedisAddress(redlockRedisC)
        );
    }

    private String toRedisAddress(GenericContainer<?> container) {
        return "redis://" + container.getHost() + ":" + container.getMappedPort(6379);
    }

    private void flushAll(StringRedisTemplate template) {
        RedisConnection connection = template.getConnectionFactory().getConnection();
        try {
            connection.serverCommands().flushAll();
        } finally {
            connection.close();
        }
    }

    private WordAnalysisResult sample(String originalForm) {
        return WordAnalysisResult.builder()
                .originalForm(originalForm)
                .sourceLanguageCode(LanguageCode.EN)
                .targetLanguageCode(LanguageCode.KO)
                .build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private record CoordinatorFixture(
            JedisConnectionFactory connectionFactory,
            StringRedisTemplate template,
            RedisMessageListenerContainer listenerContainer,
            RedissonClient redissonClient,
            WordSingleFlightRedisCoordinator coordinator
    ) {
        void close() {
            try {
                ReflectionTestUtils.invokeMethod(coordinator, "shutdown");
            } catch (Exception ignored) {
            }
            try {
                listenerContainer.stop();
            } catch (Exception ignored) {
            }
            try {
                redissonClient.shutdown();
            } catch (Exception ignored) {
            }
            try {
                connectionFactory.destroy();
            } catch (Exception ignored) {
            }
        }
    }
}
