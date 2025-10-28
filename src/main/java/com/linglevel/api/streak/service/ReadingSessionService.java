package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.ReadingSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String READING_SESSION_KEY_PREFIX = "user:";
    private static final String READING_SESSION_KEY_SUFFIX = ":reading_session";
    private static final Duration READING_SESSION_TTL = Duration.ofHours(6);
    private static final Duration MIN_READING_DURATION = Duration.ofSeconds(30);

    public void startReadingSession(String userId, ContentType contentType, String contentId) {
        String key = READING_SESSION_KEY_PREFIX + userId + READING_SESSION_KEY_SUFFIX;
        ReadingSession session = ReadingSession.builder()
                .contentType(contentType)
                .contentId(contentId)
                .startedAt(Instant.now())
                .build();

        redisTemplate.opsForValue().set(key, session, READING_SESSION_TTL);
        log.info("Reading session started for user: {}, content: {}/{}", userId, contentType.getCode(), contentId);
    }

    public ReadingSession getReadingSession(String userId) {
        String key = READING_SESSION_KEY_PREFIX + userId + READING_SESSION_KEY_SUFFIX;
        return (ReadingSession) redisTemplate.opsForValue().get(key);
    }

    public void deleteReadingSession(String userId) {
        String key = READING_SESSION_KEY_PREFIX + userId + READING_SESSION_KEY_SUFFIX;
        redisTemplate.delete(key);
    }

    public boolean isReadingSessionValid(String userId, ContentType contentType, String contentId) {
        ReadingSession session = getReadingSession(userId);
        if (session == null) {
            log.info("No reading session found for user: {}", userId);
            return false;
        }

        if (!session.getContentType().equals(contentType) || !session.getContentId().equals(contentId)) {
            log.info("Reading session content mismatch for user: {}. Expected: {}/{}, Actual: {}/{}",
                    userId, contentType.getCode(), contentId, session.getContentType().getCode(), session.getContentId());
            return false;
        }

        Duration readingDuration = Duration.between(session.getStartedAt(), Instant.now());
        if (readingDuration.compareTo(MIN_READING_DURATION) < 0) {
            log.info("Reading duration for user: {} is less than 30 seconds. Duration: {}", userId, readingDuration);
            return false;
        }

        return true;
    }
}
