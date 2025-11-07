package com.linglevel.api.content.feed.scheduler;

import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.content.feed.service.FeedCrawlingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedCrawlingScheduler {

    private final FeedSourceRepository feedSourceRepository;
    private final FeedCrawlingService feedCrawlingService;

    /**
     * 매일 새벽 3시에 활성화된 모든 FeedSource 크롤링 (한국 시간 기준)
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void scheduledCrawling() {
        log.info("Scheduled crawling started at 3 AM");

        List<FeedSource> sources = feedSourceRepository.findByIsActiveTrue();

        log.info("Found {} active FeedSources to crawl", sources.size());

        int totalCrawled = 0;
        for (FeedSource source : sources) {
            int count = feedCrawlingService.crawlFeedSource(source);
            totalCrawled += count;
        }

        log.info("Scheduled crawling completed: {} feeds collected", totalCrawled);
    }

    /**
     * 수동 트리거: 모든 활성화된 FeedSource 즉시 크롤링
     */
    public int crawlAllSources() {
        log.info("Manual crawling triggered for all sources");

        List<FeedSource> sources = feedSourceRepository.findByIsActiveTrue();
        int totalCrawled = 0;

        for (FeedSource source : sources) {
            int count = feedCrawlingService.crawlFeedSource(source);
            totalCrawled += count;
        }

        log.info("Manual crawling completed: {} feeds collected", totalCrawled);
        return totalCrawled;
    }

    /**
     * 수동 트리거: 특정 FeedSource만 크롤링
     */
    public int crawlSingleSource(String feedSourceId) {
        FeedSource source = feedSourceRepository.findById(feedSourceId)
            .orElseThrow(() -> new IllegalArgumentException("FeedSource not found: " + feedSourceId));

        return feedCrawlingService.crawlFeedSource(source);
    }
}
