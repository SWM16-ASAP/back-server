package com.linglevel.api.content.recommendation.event;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.repository.FeedRepository;
import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import com.linglevel.api.content.recommendation.repository.ContentAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentAccessEventListener {

    private final ContentAccessLogRepository contentAccessLogRepository;
    private final FeedRepository feedRepository;
    private final CustomContentRepository customContentRepository;

    @Async
    @EventListener
    public void handleContentAccessEvent(ContentAccessEvent event) {
        try {
            // CustomContent인 경우 Feed에서 category 가져오기
            var category = event.getCategory();
            if (event.getContentType() == ContentType.CUSTOM && category == null) {
                category = getCategoryFromFeed(event.getContentId());
            }

            // 1. ContentAccessLog 저장 (readTimeSeconds 포함)
            ContentAccessLog accessLog = ContentAccessLog.builder()
                    .userId(event.getUserId())
                    .contentId(event.getContentId())
                    .contentType(event.getContentType())
                    .category(category)
                    .readTimeSeconds(event.getReadTimeSeconds())
                    .accessedAt(Instant.now())
                    .build();

            contentAccessLogRepository.save(accessLog);
            log.debug("Content access logged: userId={}, contentId={}, contentType={}, category={}, readTimeSeconds={}",
                    event.getUserId(), event.getContentId(), event.getContentType(),
                    category, event.getReadTimeSeconds());

            // 2. CustomContent인 경우 Feed의 avgReadTimeSeconds 업데이트
            if (event.getContentType() == ContentType.CUSTOM && event.getReadTimeSeconds() != null) {
                updateFeedAvgReadTime(event.getContentId(), event.getReadTimeSeconds());
            }
        } catch (Exception e) {
            log.error("Failed to log content access", e);
        }
    }

    /**
     * CustomContent의 originUrl로 Feed의 category를 가져옴
     */
    private ContentCategory getCategoryFromFeed(String customContentId) {
        try {
            CustomContent customContent = customContentRepository.findById(customContentId).orElse(null);
            if (customContent == null || customContent.getOriginUrl() == null || customContent.getOriginUrl().isEmpty()) {
                return null;
            }

            Feed feed = feedRepository.findByUrl(customContent.getOriginUrl()).orElse(null);
            if (feed == null) {
                return null;
            }

            return feed.getCategory();
        } catch (Exception e) {
            log.error("Failed to get category from Feed for customContentId: {}", customContentId, e);
            return null;
        }
    }

    private void updateFeedAvgReadTime(String customContentId, Integer readTimeSeconds) {
        try {
            // CustomContent 조회하여 originUrl 가져오기
            CustomContent customContent = customContentRepository.findById(customContentId).orElse(null);
            if (customContent == null || customContent.getOriginUrl() == null || customContent.getOriginUrl().isEmpty()) {
                return;
            }

            // Feed 조회 (originUrl 기반)
            Feed feed = feedRepository.findByUrl(customContent.getOriginUrl()).orElse(null);
            if (feed == null) {
                return;
            }

            // 평균 읽기 시간 갱신 (이동 평균)
            Integer currentViewCount = feed.getViewCount() != null ? feed.getViewCount() : 0;
            Double currentAvg = feed.getAvgReadTimeSeconds() != null ? feed.getAvgReadTimeSeconds() : 0.0;

            if (currentViewCount == 0) {
                // 첫 읽기
                feed.setAvgReadTimeSeconds(readTimeSeconds.doubleValue());
            } else {
                // 이동 평균 계산
                Double newAvg = ((currentAvg * currentViewCount) + readTimeSeconds) / (currentViewCount + 1.0);
                feed.setAvgReadTimeSeconds(newAvg);
            }

            feedRepository.save(feed);
            log.debug("Updated Feed avgReadTimeSeconds: feedId={}, newAvg={}", feed.getId(), feed.getAvgReadTimeSeconds());
        } catch (Exception e) {
            log.error("Failed to update Feed avgReadTimeSeconds for customContentId: {}", customContentId, e);
        }
    }
}
