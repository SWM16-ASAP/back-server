package com.linglevel.api.word.service;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordSingleFlightRedisCoordinator {

	private static final String LOCK_PREFIX = "sf:word:lock";

	private static final String DONE_PREFIX = "sf:word:done";

	private static final String DONE_PATTERN = DONE_PREFIX + ":*";

	private final StringRedisTemplate stringRedisTemplate;

	private final RedisMessageListenerContainer redisMessageListenerContainer;

	private final RedissonClient redissonClient;

	private final WordSingleFlightProperties properties;

	private final ConcurrentHashMap<String, CopyOnWriteArrayList<CompletableFuture<Void>>> channelWaiters = new ConcurrentHashMap<>();

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
		if (!properties.isEnabled()) {
			return leaderAction.get();
		}

		KeySet keys = buildKeySet(word, targetLanguage);
		RLock lock = createLock(keys.lockKey());
		boolean lockAcquired = tryAcquireLeaderLock(lock);
		if (lockAcquired) {
			return executeWithLeaderLock(keys, lock, leaderAction, followerResultLookup);
		}

		return waitAsFollower(keys, lock, leaderAction, followerResultLookup);
	}

	private <T> T executeWithLeaderLock(KeySet keys, RLock lock, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup) {
		Optional<T> existing;
		try {
			existing = followerResultLookup.get();
		}
		catch (RuntimeException | Error e) {
			releaseLock(lock, keys.lockKey());
			throw e;
		}

		if (existing.isPresent()) {
			releaseThenPublishDone(keys, lock);
			return existing.get();
		}

		return executeAsLeader(keys, lock, leaderAction);
	}

	private <T> T executeAsLeader(KeySet keys, RLock lock, Supplier<T> leaderAction) {
		T result;
		try {
			result = leaderAction.get();
		}
		catch (RuntimeException | Error e) {
			completeLeaderAfterCompletion(keys, lock);
			throw e;
		}

		completeLeaderAfterCommit(keys, lock);
		return result;
	}

	private boolean tryAcquireLeaderLock(RLock lock) {
		try {
			// Use Redisson watchdog mode (no fixed lease time) to keep lock alive
			// while leaderAction is still running, and release promptly on unlock.
			return lock.tryLock(0, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while acquiring single-flight lock", e);
		}
	}

	private <T> T waitAsFollower(KeySet keys, RLock lock, Supplier<T> leaderAction,
			Supplier<Optional<T>> followerResultLookup) {
		CompletableFuture<Void> signal = new CompletableFuture<>();
		registerWaiter(keys.channel(), signal);

		try {
			boolean lockAcquiredAfterRegister = tryAcquireLeaderLock(lock);
			if (lockAcquiredAfterRegister) {
				return executeWithLeaderLock(keys, lock, leaderAction, followerResultLookup);
			}

			signal.get(properties.getWaitTimeoutMs(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException e) {
			log.warn("Single-flight wait timed out for key digest={}", keys.digest());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Single-flight wait interrupted for key digest=" + keys.digest(), e);
		}
		catch (ExecutionException e) {
			throw new RuntimeException("Single-flight wait failed for key digest=" + keys.digest(), e);
		}
		finally {
			unregisterWaiter(keys.channel(), signal);
		}

		Optional<T> finalResult = followerResultLookup.get();
		if (finalResult.isPresent()) {
			return finalResult.get();
		}

		throw new WordsException(WordsErrorCode.WORD_ANALYSIS_TIMEOUT);
	}

	private void completeLeaderAfterCommit(KeySet keys, RLock lock) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			releaseThenPublishDone(keys, lock);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				releaseThenPublishDone(keys, lock);
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
			releaseThenPublishDone(keys, lock);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				releaseThenPublishDone(keys, lock);
			}
		});
	}

	private void releaseThenPublishDone(KeySet keys, RLock lock) {
		releaseLock(lock, keys.lockKey());
		publishDone(keys.channel());
	}

	private void publishDone(String channel) {
		stringRedisTemplate.convertAndSend(channel, "done");
	}

	private void releaseLock(RLock lock, String lockKey) {
		try {
			lock.unlock();
		}
		catch (IllegalMonitorStateException e) {
			log.warn("Single-flight lock was not held at release time key={}", lockKey, e);
		}
		catch (Exception e) {
			log.warn("Failed to release single-flight lock key={}", lockKey, e);
		}
	}

	private RLock createLock(String lockKey) {
		return redissonClient.getLock(lockKey);
	}

	private void registerWaiter(String channel, CompletableFuture<Void> signal) {
		channelWaiters.compute(channel, (key, waiters) -> {
			CopyOnWriteArrayList<CompletableFuture<Void>> values = waiters == null ? new CopyOnWriteArrayList<>()
					: waiters;
			values.add(signal);
			return values;
		});
	}

	private void unregisterWaiter(String channel, CompletableFuture<Void> signal) {
		channelWaiters.computeIfPresent(channel, (key, waiters) -> {
			waiters.remove(signal);
			return waiters.isEmpty() ? null : waiters;
		});
	}

	private void onDoneMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		List<CompletableFuture<Void>> waiters = channelWaiters.remove(channel);
		if (waiters == null || waiters.isEmpty()) {
			return;
		}

		for (CompletableFuture<Void> waiter : waiters) {
			waiter.complete(null);
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

}
