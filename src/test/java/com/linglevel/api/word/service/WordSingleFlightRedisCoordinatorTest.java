package com.linglevel.api.word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> redisStore = new ConcurrentHashMap<>();

    private WordSingleFlightProperties properties;
    private WordSingleFlightRedisCoordinator coordinator;

    @BeforeEach
    void setUp() {
        properties = new WordSingleFlightProperties();
        properties.setEnabled(true);
        properties.setLockTtlMs(1_000);
        properties.setWaitTimeoutMs(120);
        properties.setResultTtlMs(2_000);
        properties.setPromptVersion("v1");
        properties.setModel("test-model");
        properties.setSchemaVersion("v2");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redissonClient.getLock(anyString())).thenReturn(redissonLock);
        when(redissonLock.isHeldByCurrentThread()).thenReturn(true);

        doAnswer(invocation -> redisStore.get(invocation.getArgument(0)))
                .when(valueOperations).get(anyString());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            redisStore.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        coordinator = new WordSingleFlightRedisCoordinator(
                stringRedisTemplate,
                redisMessageListenerContainer,
                redissonClient,
                properties,
                objectMapper
        );
        ReflectionTestUtils.invokeMethod(coordinator, "initialize");
    }

    @Test
    @DisplayName("동일 키 동시 요청은 leader action을 한 번만 실행한다")
    void execute_deduplicatesConcurrentRequests() throws Exception {
        stubTryLock(true, false);

        AtomicInteger aiCalls = new AtomicInteger();
        WordAnalysisResult sample = WordAnalysisResult.builder()
                .originalForm("run")
                .targetLanguageCode(LanguageCode.KO)
                .sourceLanguageCode(LanguageCode.EN)
                .build();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<WordAnalysisResult>> f1 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return coordinator.execute("run", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    sleep(50);
                    return List.of(sample);
                });
            });

            Future<List<WordAnalysisResult>> f2 = executor.submit(() -> {
                start.await(1, TimeUnit.SECONDS);
                return coordinator.execute("run", LanguageCode.KO, () -> {
                    aiCalls.incrementAndGet();
                    return List.of(sample);
                });
            });

            start.countDown();

            List<WordAnalysisResult> r1 = f1.get(2, TimeUnit.SECONDS);
            List<WordAnalysisResult> r2 = f2.get(2, TimeUnit.SECONDS);

            assertThat(r1).hasSize(1);
            assertThat(r2).hasSize(1);
            assertThat(aiCalls.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("알림 유실 상황에서도 timeout 이후 resultKey 재조회로 결과를 반환한다")
    void execute_fallbacksToResultKeyAfterTimeout() {
        stubTryLock(false);

        redisStore.clear();

        WordAnalysisResult sample = WordAnalysisResult.builder()
                .originalForm("book")
                .targetLanguageCode(LanguageCode.KO)
                .sourceLanguageCode(LanguageCode.EN)
                .build();

        String serialized = toSuccessEnvelopeJson(sample);
        AtomicInteger getCalls = new AtomicInteger();

        doAnswer(invocation -> {
            // execute() 내부 readResult 호출 순서:
            // 1) 캐시 확인 -> null
            // 2) follower 진입 후 pre-check -> null
            // 3) register 후 post-check -> null
            // 4) timeout 후 final-check -> success
            int n = getCalls.incrementAndGet();
            if (n < 4) {
                return null;
            }
            return serialized;
        }).when(valueOperations).get(anyString());

        List<WordAnalysisResult> result = coordinator.execute(
                "book",
                LanguageCode.KO,
                () -> {
                    throw new IllegalStateException("follower path should not run leader action");
                }
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalForm()).isEqualTo("book");
        assertThat(getCalls.get()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("leader 실패 결과는 같은 키 요청에 동일하게 전파된다")
    void execute_propagatesLeaderFailure() {
        stubTryLock(true, false);

        RuntimeException failure = new RuntimeException("bedrock failure");

        assertThatThrownBy(() ->
                coordinator.execute("left", LanguageCode.KO, () -> {
                    throw failure;
                })
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("bedrock failure");

        assertThatThrownBy(() ->
                coordinator.execute("left", LanguageCode.KO, ArrayList::new)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Single-flight leader failed");
    }

    @Test
    @DisplayName("leader의 WordsErrorCode는 follower leader-failure 예외로 전달된다")
    void execute_propagatesLeaderDomainErrorCode() {
        stubTryLock(true, false);

        RuntimeException failure = new RuntimeException("wrapped", new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS));

        assertThatThrownBy(() ->
                coordinator.execute("typooo", LanguageCode.KO, () -> {
                    throw failure;
                })
        ).isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() ->
                coordinator.execute("typooo", LanguageCode.KO, ArrayList::new)
        ).isInstanceOf(WordSingleFlightLeaderFailureException.class)
         .satisfies(ex -> assertThat(((WordSingleFlightLeaderFailureException) ex).getLeaderErrorCode())
                 .isEqualTo(WordsErrorCode.WORD_IS_MEANINGLESS));
    }

    @Test
    @DisplayName("Redlock 활성 + 3개 노드 구성 시 RedissonRedLock을 사용한다")
    void createLeaderLock_usesRedlockWhenConfigured() {
        properties.setRedlockEnabled(true);
        properties.setRedlockNodeAddresses(List.of(
                "redis://127.0.0.1:6379",
                "redis://127.0.0.1:6380",
                "redis://127.0.0.1:6381"
        ));

        ReflectionTestUtils.invokeMethod(coordinator, "initializeRedlockClients");
        try {
            RLock lock = ReflectionTestUtils.invokeMethod(coordinator, "createLeaderLock", "sf:word:lock:redlock");
            assertThat(lock).isInstanceOf(RedissonRedLock.class);
            verify(redissonClient, never()).getLock("sf:word:lock:redlock");
        } finally {
            ReflectionTestUtils.invokeMethod(coordinator, "shutdown");
        }
    }

    @Test
    @DisplayName("Redlock 활성 + 노드 2개 구성 시 single RLock으로 폴백한다")
    void createLeaderLock_fallsBackToSingleRLockWhenInsufficientNodes() {
        properties.setRedlockEnabled(true);
        properties.setRedlockNodeAddresses(List.of(
                "redis://127.0.0.1:6379",
                "redis://127.0.0.1:6380"
        ));

        ReflectionTestUtils.invokeMethod(coordinator, "initializeRedlockClients");
        try {
            RLock lock = ReflectionTestUtils.invokeMethod(coordinator, "createLeaderLock", "sf:word:lock:fallback");
            assertThat(lock).isSameAs(redissonLock);
            verify(redissonClient).getLock("sf:word:lock:fallback");
        } finally {
            ReflectionTestUtils.invokeMethod(coordinator, "shutdown");
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String toSuccessEnvelopeJson(WordAnalysisResult result) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("results", List.of(result));
            payload.put("errorMessage", null);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
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
            when(redissonLock.tryLock(0, TimeUnit.MILLISECONDS))
                    .thenReturn(sequence[0], java.util.Arrays.copyOfRange(sequence, 1, sequence.length));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
