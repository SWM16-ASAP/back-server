package com.linglevel.api.streak.service;

import com.linglevel.api.common.AbstractRedisTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.ReadingSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class ReadingSessionServiceTest extends AbstractRedisTest {

    private ReadingSessionService readingSessionService;
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_BOOK_ID = "test-book-456";
    private static final String TEST_ARTICLE_ID = "test-article-789";

    @BeforeEach
    void setup() {
        // Redis 연결 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(getRedisContainer().getHost());
        config.setPort(getRedisContainer().getMappedPort(6379));

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        // RedisTemplate 설정
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        // ReadingSessionService 생성
        readingSessionService = new ReadingSessionService(redisTemplate);
    }

    @AfterEach
    void cleanup() {
        // 테스트 후 Redis 정리
        if (readingSessionService != null) {
            readingSessionService.deleteReadingSession(TEST_USER_ID);
        }
    }

    @Test
    @DisplayName("읽기 세션 시작 - Redis에 저장 확인")
    void startReadingSession_Success() {
        // given
        ContentType contentType = ContentType.BOOK;
        String contentId = TEST_BOOK_ID;

        // when
        readingSessionService.startReadingSession(TEST_USER_ID, contentType, contentId);

        // then
        ReadingSession session = readingSessionService.getReadingSession(TEST_USER_ID);
        assertThat(session).isNotNull();
        assertThat(session.getContentType()).isEqualTo(contentType);
        assertThat(session.getContentId()).isEqualTo(contentId);
        assertThat(session.getStartedAtMillis()).isNotNull();
        assertThat(session.getStartedAtMillis()).isLessThanOrEqualTo(System.currentTimeMillis());
    }

    @Test
    @DisplayName("읽기 세션 조회 - 세션이 없으면 null 반환")
    void getReadingSession_NotFound() {
        // when
        ReadingSession session = readingSessionService.getReadingSession("non-existent-user");

        // then
        assertThat(session).isNull();
    }

    @Test
    @DisplayName("읽기 세션 검증 - 30초 이상 경과 시 true")
    void isReadingSessionValid_After30Seconds() {
        // given - 31초 전에 시작한 세션을 직접 생성
        ContentType contentType = ContentType.BOOK;
        String contentId = TEST_BOOK_ID;
        long thirtyOneSecondsAgo = System.currentTimeMillis() - 31_000;

        ReadingSession session = ReadingSession.builder()
                .contentType(contentType)
                .contentId(contentId)
                .startedAtMillis(thirtyOneSecondsAgo)
                .build();

        String key = "user:" + TEST_USER_ID + ":reading_session";
        redisTemplate.opsForValue().set(key, session);

        // when
        boolean isValid = readingSessionService.isReadingSessionValid(TEST_USER_ID, contentType, contentId);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("읽기 세션 검증 - 5초 미만 경과 시 false")
    void isReadingSessionValid_Before5Seconds() {
        // given - 10초 전에 시작한 세션을 직접 생성
        ContentType contentType = ContentType.BOOK;
        String contentId = TEST_BOOK_ID;
        long tenSecondsAgo = System.currentTimeMillis() - 1_000;

        ReadingSession session = ReadingSession.builder()
                .contentType(contentType)
                .contentId(contentId)
                .startedAtMillis(tenSecondsAgo)
                .build();

        String key = "user:" + TEST_USER_ID + ":reading_session";
        redisTemplate.opsForValue().set(key, session);

        // when
        boolean isValid = readingSessionService.isReadingSessionValid(TEST_USER_ID, contentType, contentId);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("읽기 세션 검증 - 세션이 없으면 false")
    void isReadingSessionValid_NoSession() {
        // when
        boolean isValid = readingSessionService.isReadingSessionValid(
            "non-existent-user",
            ContentType.BOOK,
            TEST_BOOK_ID
        );

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("읽기 세션 검증 - ContentType이 다르면 false")
    void isReadingSessionValid_DifferentContentType() {
        // given - 31초 전에 시작한 BOOK 세션
        long thirtyOneSecondsAgo = System.currentTimeMillis() - 31_000;
        ReadingSession session = ReadingSession.builder()
                .contentType(ContentType.BOOK)
                .contentId(TEST_BOOK_ID)
                .startedAtMillis(thirtyOneSecondsAgo)
                .build();

        String key = "user:" + TEST_USER_ID + ":reading_session";
        redisTemplate.opsForValue().set(key, session);

        // when - ARTICLE로 검증 시도
        boolean isValid = readingSessionService.isReadingSessionValid(
            TEST_USER_ID,
            ContentType.ARTICLE,
            TEST_BOOK_ID
        );

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("읽기 세션 검증 - ContentId가 다르면 false")
    void isReadingSessionValid_DifferentContentId() {
        // given - 31초 전에 시작한 세션
        long thirtyOneSecondsAgo = System.currentTimeMillis() - 31_000;
        ReadingSession session = ReadingSession.builder()
                .contentType(ContentType.BOOK)
                .contentId(TEST_BOOK_ID)
                .startedAtMillis(thirtyOneSecondsAgo)
                .build();

        String key = "user:" + TEST_USER_ID + ":reading_session";
        redisTemplate.opsForValue().set(key, session);

        // when - 다른 bookId로 검증 시도
        boolean isValid = readingSessionService.isReadingSessionValid(
            TEST_USER_ID,
            ContentType.BOOK,
            "different-book-id"
        );

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("읽기 세션 덮어쓰기 - A 작품 → B 작품 전환")
    void startReadingSession_Override() {
        // given - Book A 세션 시작
        readingSessionService.startReadingSession(TEST_USER_ID, ContentType.BOOK, TEST_BOOK_ID);

        // when - Article B 세션 시작 (덮어쓰기)
        readingSessionService.startReadingSession(TEST_USER_ID, ContentType.ARTICLE, TEST_ARTICLE_ID);

        // then - Article B 세션만 존재
        ReadingSession session = readingSessionService.getReadingSession(TEST_USER_ID);
        assertThat(session).isNotNull();
        assertThat(session.getContentType()).isEqualTo(ContentType.ARTICLE);
        assertThat(session.getContentId()).isEqualTo(TEST_ARTICLE_ID);

        // Book A는 검증 실패 (세션이 덮어써졌으므로, ContentType이 다름)
        boolean isValidBookA = readingSessionService.isReadingSessionValid(
            TEST_USER_ID,
            ContentType.BOOK,
            TEST_BOOK_ID
        );
        assertThat(isValidBookA).isFalse();
    }

    @Test
    @DisplayName("읽기 세션 삭제")
    void deleteReadingSession_Success() {
        // given
        readingSessionService.startReadingSession(TEST_USER_ID, ContentType.BOOK, TEST_BOOK_ID);
        assertThat(readingSessionService.getReadingSession(TEST_USER_ID)).isNotNull();

        // when
        readingSessionService.deleteReadingSession(TEST_USER_ID);

        // then
        assertThat(readingSessionService.getReadingSession(TEST_USER_ID)).isNull();
    }

    @Test
    @DisplayName("Redis 직렬화 테스트 - ReadingSession 저장/조회")
    void testRedisSerialization() {
        // given
        String key = "test:serialization";
        ReadingSession session = ReadingSession.builder()
            .contentType(ContentType.BOOK)
            .contentId(TEST_BOOK_ID)
            .startedAtMillis(System.currentTimeMillis())
            .build();

        // when
        redisTemplate.opsForValue().set(key, session);
        Object retrieved = redisTemplate.opsForValue().get(key);

        // then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isInstanceOf(ReadingSession.class);
        ReadingSession retrievedSession = (ReadingSession) retrieved;
        assertThat(retrievedSession.getContentType()).isEqualTo(ContentType.BOOK);
        assertThat(retrievedSession.getContentId()).isEqualTo(TEST_BOOK_ID);
        assertThat(retrievedSession.getStartedAtMillis()).isEqualTo(session.getStartedAtMillis());

        // cleanup
        redisTemplate.delete(key);
    }
}
