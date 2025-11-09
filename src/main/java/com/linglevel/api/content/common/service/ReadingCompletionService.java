package com.linglevel.api.content.common.service;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.recommendation.event.ContentAccessEvent;
import com.linglevel.api.streak.service.ReadingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 콘텐츠 읽기 완료 처리 공통 서비스
 *
 * 모든 콘텐츠 타입(Article, CustomContent, Book)에서 공통으로 사용하는
 * 읽기 완료 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingCompletionService {

    private final ReadingSessionService readingSessionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 읽기 완료 처리 (30초 이상 읽은 경우만)
     *
     * @param userId 사용자 ID
     * @param contentType 콘텐츠 타입
     * @param contentId 콘텐츠 ID
     * @param category 카테고리 (nullable, Book은 null)
     * @return 읽은 시간(초), 30초 미만이면 null
     */
    public Long processReadingCompletion (
            String userId,
            ContentType contentType,
            String contentId,
            ContentCategory category) {

        boolean sessionValid = readingSessionService.isReadingSessionValid(userId, contentType, contentId);

        if (!sessionValid) {
            return null;
        }

        long readTimeSeconds = readingSessionService.getReadingSessionSeconds(userId, contentType, contentId);

        eventPublisher.publishEvent(new ContentAccessEvent(
                this,
                userId,
                contentId,
                contentType,
                category,
                (int) readTimeSeconds
        ));

        readingSessionService.deleteReadingSession(userId);

        return readTimeSeconds;
    }
}