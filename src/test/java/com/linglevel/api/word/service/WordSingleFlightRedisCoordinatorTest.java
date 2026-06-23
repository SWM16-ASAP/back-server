package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.config.WordSingleFlightProperties;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WordSingleFlightRedisCoordinatorTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private RedisMessageListenerContainer redisMessageListenerContainer;

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock redissonLock;

	private WordSingleFlightProperties properties;

	private WordSingleFlightRedisCoordinator coordinator;

	@BeforeEach
	void setUp() {
		properties = new WordSingleFlightProperties();
		properties.setEnabled(true);
		properties.setWaitTimeoutMs(120);
		properties.setResultSchemaVersion("v2");

		when(redissonClient.getLock(anyString())).thenReturn(redissonLock);
		when(redissonLock.isHeldByCurrentThread()).thenReturn(true);

		coordinator = new WordSingleFlightRedisCoordinator(stringRedisTemplate, redisMessageListenerContainer,
				redissonClient, properties);
		ReflectionTestUtils.invokeMethod(coordinator, "initialize");
	}

	@Test
	@DisplayName("동일 키 동시 요청은 leader action을 한 번만 실행하고 follower는 조회 함수 결과를 반환한다")
	void execute_deduplicatesConcurrentRequests() throws Exception {
		stubTryLock(true, false);

		AtomicInteger aiCalls = new AtomicInteger();
		AtomicReference<List<WordAnalysisResult>> stored = new AtomicReference<>();
		WordAnalysisResult sample = sample("run");

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<List<WordAnalysisResult>> f1 = executor.submit(() -> {
				start.await(1, TimeUnit.SECONDS);
				return coordinator.execute("run", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					sleep(50);
					List<WordAnalysisResult> result = List.of(sample);
					stored.set(result);
					return result;
				}, () -> Optional.ofNullable(stored.get()));
			});

			Future<List<WordAnalysisResult>> f2 = executor.submit(() -> {
				start.await(1, TimeUnit.SECONDS);
				return coordinator.execute("run", LanguageCode.KO, () -> {
					aiCalls.incrementAndGet();
					List<WordAnalysisResult> result = List.of(sample);
					stored.set(result);
					return result;
				}, () -> Optional.ofNullable(stored.get()));
			});

			start.countDown();

			List<WordAnalysisResult> r1 = f1.get(2, TimeUnit.SECONDS);
			List<WordAnalysisResult> r2 = f2.get(2, TimeUnit.SECONDS);

			assertThat(r1).hasSize(1);
			assertThat(r2).hasSize(1);
			assertThat(aiCalls.get()).isEqualTo(1);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	@DisplayName("알림 유실 상황에서도 timeout 이후 조회 함수로 DB 결과를 반환한다")
	void execute_fallbacksToLookupAfterTimeout() {
		stubTryLock(false);

		WordAnalysisResult sample = sample("book");
		AtomicInteger lookupCalls = new AtomicInteger();

		List<WordAnalysisResult> result = coordinator.execute("book", LanguageCode.KO, () -> {
			throw new IllegalStateException("follower path should not run leader action");
		}, () -> {
			lookupCalls.incrementAndGet();
			return Optional.of(List.of(sample));
		});

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOriginalForm()).isEqualTo("book");
		assertThat(lookupCalls.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("leader 실패 후 DB 결과가 없으면 follower는 timeout으로 실패한다")
	void execute_followerTimesOutWhenLeaderFailsWithoutStoredResult() {
		stubTryLock(true, false);

		RuntimeException failure = new RuntimeException("bedrock failure");

		assertThatThrownBy(() -> coordinator.execute("left", LanguageCode.KO, () -> {
			throw failure;
		}, Optional::empty)).isSameAs(failure);

		assertThatThrownBy(() -> coordinator.execute("left", LanguageCode.KO, () -> {
			throw new IllegalStateException("follower path should not run leader action");
		}, Optional::empty)).isInstanceOf(WordsException.class)
			.satisfies(ex -> assertThat(((WordsException) ex).getErrorCode())
				.isEqualTo(WordsErrorCode.WORD_ANALYSIS_TIMEOUT));
	}

	@Test
	@DisplayName("follower는 leader lock이 유지되는 동안 DB 조회 함수를 실행하지 않는다")
	void execute_doesNotRunFollowerLookupWhileLeaderLockIsHeld() {
		stubTryLock(false);

		AtomicInteger lookupCalls = new AtomicInteger();

		assertThatThrownBy(() -> coordinator.execute("saw", LanguageCode.KO, () -> {
			throw new IllegalStateException("follower path should not run leader action");
		}, () -> {
			lookupCalls.incrementAndGet();
			return Optional.empty();
		})).isInstanceOf(WordsException.class)
			.satisfies(ex -> assertThat(((WordsException) ex).getErrorCode())
				.isEqualTo(WordsErrorCode.WORD_ANALYSIS_TIMEOUT));

		assertThat(lookupCalls.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("follower 조회 함수의 예외는 그대로 전파된다")
	void execute_propagatesFollowerLookupException() {
		stubTryLock(false);

		IllegalStateException failure = new IllegalStateException("db lookup failed");

		assertThatThrownBy(() -> coordinator.execute("typooo", LanguageCode.KO, () -> {
			throw new IllegalStateException("follower path should not run leader action");
		}, () -> {
			throw failure;
		})).isSameAs(failure);
	}

	@Test
	@DisplayName("leader 완료 시 lock을 해제한 뒤 done을 발행한다")
	void execute_releasesLeaderLockBeforePublishingDone() {
		stubTryLock(true);

		List<WordAnalysisResult> result = coordinator.execute("run", LanguageCode.KO, () -> List.of(sample("run")),
				Optional::empty);

		assertThat(result).hasSize(1);
		InOrder inOrder = inOrder(redissonLock, stringRedisTemplate);
		inOrder.verify(redissonLock).unlock();
		inOrder.verify(stringRedisTemplate).convertAndSend(anyString(), anyString());
	}

	@Test
	@DisplayName("done publish가 실패해도 leader lock은 먼저 해제되어 있다")
	void execute_releasesLeaderLockBeforePublishFailure() {
		stubTryLock(true);

		RuntimeException publishFailure = new RuntimeException("redis publish failed");
		doThrow(publishFailure).when(stringRedisTemplate).convertAndSend(anyString(), anyString());

		assertThatThrownBy(
				() -> coordinator.execute("run", LanguageCode.KO, () -> List.of(sample("run")), Optional::empty))
			.isSameAs(publishFailure);

		InOrder inOrder = inOrder(redissonLock, stringRedisTemplate);
		inOrder.verify(redissonLock).unlock();
		inOrder.verify(stringRedisTemplate).convertAndSend(anyString(), anyString());
	}

	@Test
	@DisplayName("lock holder가 기존 결과를 발견하면 대기 중인 follower를 깨우도록 done을 발행한다")
	void execute_publishesDoneWhenLockHolderFindsExistingResult() {
		stubTryLock(true);

		WordAnalysisResult sample = sample("run");

		List<WordAnalysisResult> result = coordinator.execute("run", LanguageCode.KO, () -> {
			throw new IllegalStateException("leader action should not run when result already exists");
		}, () -> Optional.of(List.of(sample)));

		assertThat(result).hasSize(1);
		InOrder inOrder = inOrder(redissonLock, stringRedisTemplate);
		inOrder.verify(redissonLock).unlock();
		inOrder.verify(stringRedisTemplate).convertAndSend(anyString(), anyString());
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

	private void stubTryLock(boolean first, boolean... others) {
		Boolean[] sequence = new Boolean[others.length + 1];
		sequence[0] = first;
		for (int i = 0; i < others.length; i++) {
			sequence[i + 1] = others[i];
		}

		try {
			when(redissonLock.tryLock(0, TimeUnit.MILLISECONDS)).thenReturn(sequence[0],
					Arrays.copyOfRange(sequence, 1, sequence.length));
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
