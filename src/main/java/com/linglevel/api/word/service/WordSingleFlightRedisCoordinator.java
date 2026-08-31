package com.linglevel.api.word.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.config.WordSingleFlightProperties;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Timer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordSingleFlightRedisCoordinator {

	private static final String LOCK_PREFIX = "sf:word:lock";

	private static final String DONE_PREFIX = "sf:word:done";

	private static final String DONE_PATTERN = DONE_PREFIX + ":*";

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper;

	private final RedisMessageListenerContainer redisMessageListenerContainer;

	private final RedissonClient redissonClient;

	private final WordSingleFlightProperties properties;

	private final WordGenerationMetrics metrics;

	private final ConcurrentHashMap<String, CopyOnWriteArrayList<CompletableFuture<CompletionSignal>>> channelWaiters = new ConcurrentHashMap<>();

	private final PatternTopic doneTopic = new PatternTopic(DONE_PATTERN);

	private final MessageListener doneListener = this::onDoneMessage;

	@PostConstruct
	void initialize() {
		redisMessageListenerContainer.addMessageListener(doneListener, doneTopic);
	}

	@PreDestroy
	void shutdown() {
		redisMessageListenerContainer.removeMessageListener(doneListener, doneTopic);
	}

	public <T> T execute(String word, LanguageCode targetLanguage, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup) {
		return execute(word, targetLanguage, leaderAction, followerResultLookup, null);
	}

	public <T> T execute(String word, LanguageCode targetLanguage, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup, TypeReference<T> resultType) {
		if (!properties.isEnabled()) {
			try {
				T result = leaderAction.get();
				metrics.recordSingleFlightRequest("bypass", "success");
				return result;
			}
			catch (RuntimeException | Error e) {
				metrics.recordSingleFlightRequest("bypass", "error");
				throw e;
			}
		}

		KeySet keys = buildKeySet(word, targetLanguage);
		RLock lock = createLock(keys.lockKey());
		boolean lockAcquired = tryAcquireLeaderLock(lock);
		if (lockAcquired) {
			return executeWithLeaderLock(keys, lock, leaderAction, followerResultLookup, resultType);
		}

		return waitAsFollower(keys, lock, leaderAction, followerResultLookup, resultType);
	}

	private <T> T executeWithLeaderLock(KeySet keys, RLock lock, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup, TypeReference<T> resultType) {
		Optional<T> existing;
		try {
			existing = followerResultLookup.get();
		}
		catch (RuntimeException | Error e) {
			releaseLock(lock, keys.lockKey());
			throw e;
		}

		if (existing.isPresent()) {
			metrics.recordSingleFlightRequest("lookup", "existing");
			releaseThenPublishDone(keys, lock, existing.get());
			return existing.get();
		}

		return executeAsLeader(keys, lock, leaderAction);
	}

	private <T> T executeAsLeader(KeySet keys, RLock lock, Supplier<T> leaderAction) {
		T result;
		try {
			result = leaderAction.get();
			metrics.recordSingleFlightRequest("leader", "success");
		}
		catch (RuntimeException | Error e) {
			metrics.recordSingleFlightRequest("leader", "error");
			completeLeaderAfterCompletion(keys, lock);
			throw e;
		}

		completeLeaderAfterCommit(keys, lock, result);
		return result;
	}

	private boolean tryAcquireLeaderLock(RLock lock) {
		try {
			return lock.tryLock(0, properties.getLockLeaseMs(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			metrics.recordLockFailure("acquire");
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while acquiring single-flight lock", e);
		}
		catch (RuntimeException e) {
			metrics.recordLockFailure("acquire");
			throw e;
		}
	}

	private <T> T waitAsFollower(KeySet keys, RLock lock, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup, TypeReference<T> resultType) {
		CompletableFuture<CompletionSignal> signal = new CompletableFuture<>();
		Timer.Sample waitSample = metrics.startFollowerWait();
		registerWaiter(keys.channel(), signal);

		try {
			boolean lockAcquiredAfterRegister = tryAcquireLeaderLock(lock);
			if (lockAcquiredAfterRegister) {
				metrics.recordFollowerWait(waitSample, "promoted");
				return executeWithLeaderLock(keys, lock, leaderAction, followerResultLookup, resultType);
			}

			CompletionSignal completionSignal = signal.get(properties.getWaitTimeoutMs(), TimeUnit.MILLISECONDS);
			metrics.recordFollowerWait(waitSample, "signaled");
			T sharedResult = deserializeSharedResult(completionSignal, resultType);
			if (sharedResult != null) {
				metrics.recordSingleFlightRequest("follower", "success");
				return sharedResult;
			}
		}
		catch (TimeoutException e) {
			metrics.recordFollowerWait(waitSample, "timeout");
			log.warn("Single-flight wait timed out for key digest={}", keys.digest());
		}
		catch (InterruptedException e) {
			metrics.recordFollowerWait(waitSample, "interrupted");
			Thread.currentThread().interrupt();
			throw new RuntimeException("Single-flight wait interrupted for key digest=" + keys.digest(), e);
		}
		catch (ExecutionException e) {
			metrics.recordFollowerWait(waitSample, "error");
			throw new RuntimeException("Single-flight wait failed for key digest=" + keys.digest(), e);
		}
		finally {
			unregisterWaiter(keys.channel(), signal);
		}

		Optional<T> finalResult;
		try {
			finalResult = followerResultLookup.get();
		}
		catch (RuntimeException | Error e) {
			metrics.recordSingleFlightRequest("follower", "error");
			throw e;
		}
		if (finalResult.isPresent()) {
			metrics.recordSingleFlightRequest("follower", "success");
			return finalResult.get();
		}

		metrics.recordSingleFlightRequest("follower", "timeout");
		throw new WordsException(WordsErrorCode.WORD_ANALYSIS_TIMEOUT);
	}

	private void completeLeaderAfterCommit(KeySet keys, RLock lock, Object result) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			releaseThenPublishDone(keys, lock, result);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				releaseThenPublishDone(keys, lock, result);
			}

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					releaseLock(lock, keys.lockKey());
				}
			}
		});
	}

	private void completeLeaderAfterCompletion(KeySet keys, RLock lock) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			releaseThenPublishDone(keys, lock, null);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				releaseThenPublishDone(keys, lock, null);
			}
		});
	}

	private void releaseThenPublishDone(KeySet keys, RLock lock, Object result) {
		releaseLock(lock, keys.lockKey());
		publishDone(keys.channel(), result);
	}

	private void publishDone(String channel, Object result) {
		stringRedisTemplate.convertAndSend(channel, createCompletionMessage(result));
	}

	private void releaseLock(RLock lock, String lockKey) {
		try {
			lock.unlock();
		}
		catch (IllegalMonitorStateException e) {
			metrics.recordLockFailure("release");
			log.warn("Single-flight lock was not held at release time key={}", lockKey, e);
		}
		catch (Exception e) {
			metrics.recordLockFailure("release");
			log.warn("Failed to release single-flight lock key={}", lockKey, e);
		}
	}

	private RLock createLock(String lockKey) {
		return redissonClient.getLock(lockKey);
	}

	private void registerWaiter(String channel, CompletableFuture<CompletionSignal> signal) {
		channelWaiters.compute(channel, (key, waiters) -> {
			CopyOnWriteArrayList<CompletableFuture<CompletionSignal>> values = waiters == null
					? new CopyOnWriteArrayList<>() : waiters;
			values.add(signal);
			return values;
		});
	}

	private void unregisterWaiter(String channel, CompletableFuture<CompletionSignal> signal) {
		channelWaiters.computeIfPresent(channel, (key, waiters) -> {
			waiters.remove(signal);
			return waiters.isEmpty() ? null : waiters;
		});
	}

	private void onDoneMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		List<CompletableFuture<CompletionSignal>> waiters = channelWaiters.remove(channel);
		if (waiters == null || waiters.isEmpty()) {
			return;
		}

		CompletionSignal completionSignal = parseCompletionSignal(message);
		for (CompletableFuture<CompletionSignal> waiter : waiters) {
			waiter.complete(completionSignal);
		}
	}

	private String createCompletionMessage(Object result) {
		if (result == null) {
			return "{\"status\":\"done\"}";
		}

		try {
			return objectMapper.writeValueAsString(Map.of("status", "success", "result", result));
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to serialize single-flight completion result. Falling back to DB lookup.", e);
			return "{\"status\":\"done\"}";
		}
	}

	private CompletionSignal parseCompletionSignal(Message message) {
		try {
			JsonNode root = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
			JsonNode result = root.path("result");
			return result.isMissingNode() || result.isNull() ? CompletionSignal.withoutResult()
					: CompletionSignal.withResult(result);
		}
		catch (JsonProcessingException e) {
			log.warn("Failed to parse single-flight completion message. Falling back to DB lookup.", e);
			return CompletionSignal.withoutResult();
		}
	}

	private <T> T deserializeSharedResult(CompletionSignal completionSignal, TypeReference<T> resultType) {
		if (!completionSignal.hasResult() || resultType == null) {
			return null;
		}

		try {
			return objectMapper.convertValue(completionSignal.result(), resultType);
		}
		catch (IllegalArgumentException e) {
			log.warn("Failed to deserialize single-flight completion result. Falling back to DB lookup.", e);
			return null;
		}
	}

	private KeySet buildKeySet(String word, LanguageCode targetLanguage) {
		String normalizedWord = word.trim().toLowerCase(Locale.ROOT);
		String canonicalKey = String.join("|", "word=" + normalizedWord, "lang=" + targetLanguage.getCode(),
				"resultSchema=" + properties.getResultSchemaVersion());

		String digest = sha256(canonicalKey);
		String suffix = properties.getResultSchemaVersion() + ":" + digest;

		return new KeySet(LOCK_PREFIX + ":" + suffix, DONE_PREFIX + ":" + suffix, digest);
	}

	private String sha256(String value) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 is not available", e);
		}
	}

	private record KeySet(String lockKey, String channel, String digest) {
	}

	private record CompletionSignal(JsonNode result) {

		static CompletionSignal withResult(JsonNode result) {
			return new CompletionSignal(result);
		}

		static CompletionSignal withoutResult() {
			return new CompletionSignal(null);
		}

		boolean hasResult() {
			return result != null;
		}
	}

}
