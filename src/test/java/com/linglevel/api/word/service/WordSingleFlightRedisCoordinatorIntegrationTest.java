package com.linglevel.api.word.service;

import com.linglevel.api.common.AbstractRedisTest;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.config.WordSingleFlightProperties;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordSingleFlightRedisCoordinatorIntegrationTest extends AbstractRedisTest {

	private CoordinatorFixture nodeA;

	private CoordinatorFixture nodeB;

	@BeforeEach
	void setUp() {
		nodeA = createNode(3_000);
		nodeB = createNode(3_000);
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
	@DisplayName("실제 Redis에서 두 인스턴스 동시 요청 시 AI 호출은 1회만 수행되고 follower는 조회 결과를 반환한다")
	void deduplicatesAcrossTwoCoordinatorsUsingRealRedis() throws Exception {
		AtomicInteger aiCalls = new AtomicInteger();
		AtomicReference<List<WordAnalysisResult>> stored = new AtomicReference<>();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);

		try {
			Future<List<WordAnalysisResult>> f1 = executor.submit(() -> {
				start.await(1, TimeUnit.SECONDS);
				return nodeA.coordinator.execute("run", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					sleep(250);
					List<WordAnalysisResult> result = List.of(sample("run"));
					stored.set(result);
					return result;
				}, () -> Optional.ofNullable(stored.get()));
			});

			Future<List<WordAnalysisResult>> f2 = executor.submit(() -> {
				start.await(1, TimeUnit.SECONDS);
				return nodeB.coordinator.execute("run", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					List<WordAnalysisResult> result = List.of(sample("run"));
					stored.set(result);
					return result;
				}, () -> Optional.ofNullable(stored.get()));
			});

			start.countDown();

			List<WordAnalysisResult> r1 = f1.get(5, TimeUnit.SECONDS);
			List<WordAnalysisResult> r2 = f2.get(5, TimeUnit.SECONDS);

			assertThat(r1).hasSize(1);
			assertThat(r2).hasSize(1);
			assertThat(r1.get(0).getOriginalForm()).isEqualTo("run");
			assertThat(r2.get(0).getOriginalForm()).isEqualTo("run");
			assertThat(aiCalls.get()).isEqualTo(1);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	@DisplayName("leader 실패 후 저장 결과가 없으면 follower는 timeout으로 실패한다")
	void followerTimesOutWhenLeaderFailsWithoutStoredResultUsingRealRedis() throws Exception {
		RuntimeException leaderFailure = new RuntimeException("bedrock unavailable");
		AtomicInteger aiCalls = new AtomicInteger();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch leaderEntered = new CountDownLatch(1);

		try {
			Future<List<WordAnalysisResult>> leader = executor
				.submit(() -> nodeA.coordinator.execute("left", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					leaderEntered.countDown();
					sleep(250);
					throw leaderFailure;
				}, Optional::empty));

			assertThat(leaderEntered.await(2, TimeUnit.SECONDS)).isTrue();

			Future<List<WordAnalysisResult>> follower = executor
				.submit(() -> nodeB.coordinator.execute("left", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					return List.of(sample("left"));
				}, Optional::empty));

			assertThatThrownBy(() -> leader.get(5, TimeUnit.SECONDS)).hasCause(leaderFailure);
			assertThatThrownBy(() -> follower.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(WordsException.class);
			assertThat(aiCalls.get()).isEqualTo(1);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private CoordinatorFixture createNode(long waitTimeoutMs) {
		GenericContainer<?> redis = getRedisContainer();
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(),
				redis.getMappedPort(6379));

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
		redissonConfig.useSingleServer().setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
		RedissonClient redissonClient = Redisson.create(redissonConfig);

		WordSingleFlightProperties properties = new WordSingleFlightProperties();
		properties.setEnabled(true);
		properties.setWaitTimeoutMs(waitTimeoutMs);
		properties.setResultSchemaVersion("v2");

		WordSingleFlightRedisCoordinator coordinator = new WordSingleFlightRedisCoordinator(template, listenerContainer,
				redissonClient, properties, new WordGenerationMetrics(new SimpleMeterRegistry()));
		ReflectionTestUtils.invokeMethod(coordinator, "initialize");

		return new CoordinatorFixture(connectionFactory, template, listenerContainer, redissonClient, coordinator);
	}

	private void flushAll(StringRedisTemplate template) {
		RedisConnection connection = template.getConnectionFactory().getConnection();
		try {
			connection.serverCommands().flushAll();
		}
		finally {
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
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	private record CoordinatorFixture(JedisConnectionFactory connectionFactory, StringRedisTemplate template,
			RedisMessageListenerContainer listenerContainer, RedissonClient redissonClient,
			WordSingleFlightRedisCoordinator coordinator) {
		void close() {
			try {
				ReflectionTestUtils.invokeMethod(coordinator, "shutdown");
			}
			catch (Exception ignored) {
			}
			try {
				listenerContainer.stop();
			}
			catch (Exception ignored) {
			}
			try {
				redissonClient.shutdown();
			}
			catch (Exception ignored) {
			}
			try {
				connectionFactory.destroy();
			}
			catch (Exception ignored) {
			}
		}
	}

}
