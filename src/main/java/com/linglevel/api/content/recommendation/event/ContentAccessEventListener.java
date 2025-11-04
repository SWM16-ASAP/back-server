package com.linglevel.api.content.recommendation.event;

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

    @Async
    @EventListener
    public void handleContentAccessEvent(ContentAccessEvent event) {
        try {
            ContentAccessLog accessLog = ContentAccessLog.builder()
                    .userId(event.getUserId())
                    .contentId(event.getContentId())
                    .contentType(event.getContentType())
                    .category(event.getCategory())
                    .accessedAt(Instant.now())
                    .build();

            contentAccessLogRepository.save(accessLog);
            log.debug("Content access logged: userId={}, contentId={}, contentType={}, category={}",
                    event.getUserId(), event.getContentId(), event.getContentType(), event.getCategory());
        } catch (Exception e) {
            log.error("Failed to log content access", e);
        }
    }
}
