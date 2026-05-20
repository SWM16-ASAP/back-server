package com.linglevel.api.word.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import jakarta.annotation.PostConstruct;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordSingleFlightRedisCoordinator {

    private static final String LOCK_PREFIX = "sf:word:lock";
    private static final String RESULT_PREFIX = "sf:word:result";
    private static final String DONE_PREFIX = "sf:word:done";
    private static final String DONE_PATTERN = DONE_PREFIX + ":*";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final RedissonClient redissonClient;
    private final WordSingleFlightProperties properties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<CompletableFuture<Void>>> channelWaiters = new ConcurrentHashMap<>();

    private final MessageListener doneListener = this::onDoneMessage;

    @PostConstruct
    void subscribeDonePattern() {
        redisMessageListenerContainer.addMessageListener(doneListener, new PatternTopic(DONE_PATTERN));
    }

    public List<WordAnalysisResult> execute(
            String word,
            LanguageCode targetLanguage,
            Supplier<List<WordAnalysisResult>> leaderAction
    ) {
        if (!properties.isEnabled()) {
            return leaderAction.get();
        }

        KeySet keys = buildKeySet(word, targetLanguage);
        ResultEnvelope cached = readResult(keys.resultKey());
        if (cached != null) {
            return unwrap(cached, keys.digest());
        }

        RLock lock = redissonClient.getLock(keys.lockKey());
        boolean lockAcquired = tryAcquireLeaderLock(lock);
        if (lockAcquired) {
            return executeAsLeader(keys, lock, leaderAction);
        }

        return waitAsFollower(keys);
    }

    private List<WordAnalysisResult> executeAsLeader(
            KeySet keys,
            RLock lock,
            Supplier<List<WordAnalysisResult>> leaderAction
    ) {
        try {
            List<WordAnalysisResult> result = leaderAction.get();
            writeResult(keys.resultKey(), ResultEnvelope.success(result));
            publishDone(keys.channel());
            return result;
        } catch (RuntimeException e) {
            writeResult(keys.resultKey(), ResultEnvelope.failed(e.getMessage(), resolveLeaderErrorCode(e)));
            publishDone(keys.channel());
            throw e;
        } finally {
            releaseLock(lock, keys.lockKey());
        }
    }

    private boolean tryAcquireLeaderLock(RLock lock) {
        try {
            // Use Redisson watchdog mode (no fixed lease time) to keep lock alive
            // while leaderAction is still running, and release promptly on unlock.
            return lock.tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring single-flight lock", e);
        }
    }

    private List<WordAnalysisResult> waitAsFollower(KeySet keys) {
        ResultEnvelope current = readResult(keys.resultKey());
        if (current != null) {
            return unwrap(current, keys.digest());
        }

        CompletableFuture<Void> signal = new CompletableFuture<>();
        registerWaiter(keys.channel(), signal);

        try {
            ResultEnvelope afterRegister = readResult(keys.resultKey());
            if (afterRegister != null) {
                return unwrap(afterRegister, keys.digest());
            }

            signal.get(properties.getWaitTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Single-flight wait timed out for key digest={}", keys.digest());
        } catch (Exception e) {
            throw new RuntimeException("Single-flight wait interrupted for key digest=" + keys.digest(), e);
        } finally {
            unregisterWaiter(keys.channel(), signal);
        }

        ResultEnvelope finalResult = readResult(keys.resultKey());
        if (finalResult != null) {
            return unwrap(finalResult, keys.digest());
        }

        throw new WordSingleFlightTimeoutException(
                "Timed out waiting single-flight result for key digest=" + keys.digest()
        );
    }

    private void publishDone(String channel) {
        stringRedisTemplate.convertAndSend(channel, "done");
    }

    private void releaseLock(RLock lock, String lockKey) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("Failed to release single-flight lock key={}", lockKey, e);
        }
    }

    private ResultEnvelope readResult(String resultKey) {
        String raw = stringRedisTemplate.opsForValue().get(resultKey);
        if (raw == null) {
            return null;
        }

        try {
            return objectMapper.readValue(raw, ResultEnvelope.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize single-flight result key={}", resultKey, e);
            return null;
        }
    }

    private void writeResult(String resultKey, ResultEnvelope envelope) {
        try {
            String raw = objectMapper.writeValueAsString(envelope);
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    raw,
                    Duration.ofMillis(properties.getResultTtlMs())
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize single-flight result", e);
        }
    }

    private List<WordAnalysisResult> unwrap(ResultEnvelope envelope, String digest) {
        if (envelope.success()) {
            return envelope.results();
        }

        WordsErrorCode leaderErrorCode = parseLeaderErrorCode(envelope.errorCode());
        throw new WordSingleFlightLeaderFailureException(
                "Single-flight leader failed for key digest=" + digest + ": " + envelope.errorMessage(),
                leaderErrorCode
        );
    }

    private String resolveLeaderErrorCode(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof WordsException wordsException && wordsException.getErrorCode() != null) {
                return wordsException.getErrorCode().name();
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    private WordsErrorCode parseLeaderErrorCode(String rawErrorCode) {
        if (rawErrorCode == null || rawErrorCode.isBlank()) {
            return null;
        }

        try {
            return WordsErrorCode.valueOf(rawErrorCode);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown single-flight leader error code: {}", rawErrorCode);
            return null;
        }
    }

    private void registerWaiter(String channel, CompletableFuture<Void> signal) {
        channelWaiters.compute(channel, (key, waiters) -> {
            CopyOnWriteArrayList<CompletableFuture<Void>> values = waiters == null
                    ? new CopyOnWriteArrayList<>()
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
        String canonicalKey = String.join("|",
                "word=" + normalizedWord,
                "lang=" + targetLanguage.getCode(),
                "prompt=" + properties.getPromptVersion(),
                "model=" + properties.getModel(),
                "schema=" + properties.getSchemaVersion()
        );

        String digest = sha256(canonicalKey);
        String suffix = properties.getSchemaVersion() + ":" + digest;

        return new KeySet(
                LOCK_PREFIX + ":" + suffix,
                RESULT_PREFIX + ":" + suffix,
                DONE_PREFIX + ":" + suffix,
                digest
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is not available", e);
        }
    }

    private record KeySet(
            String lockKey,
            String resultKey,
            String channel,
            String digest
    ) { }

    private record ResultEnvelope(
            boolean success,
            List<WordAnalysisResult> results,
            String errorMessage,
            String errorCode
    ) {
        static ResultEnvelope success(List<WordAnalysisResult> results) {
            return new ResultEnvelope(true, results, null, null);
        }

        static ResultEnvelope failed(String errorMessage, String errorCode) {
            return new ResultEnvelope(false, List.of(), errorMessage, errorCode);
        }
    }
}
