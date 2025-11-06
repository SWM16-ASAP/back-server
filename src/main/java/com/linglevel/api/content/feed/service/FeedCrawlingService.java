package com.linglevel.api.content.feed.service;

import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedRepository;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.crawling.dsl.CrawlerDsl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedCrawlingService {

    private final FeedRepository feedRepository;
    private final FeedSourceRepository feedSourceRepository;

    /**
     * FeedSource를 크롤링하여 Feed 생성/업데이트
     *
     * @param feedSource 크롤링 대상
     * @return 수집된 Feed 개수
     */
    public int crawlFeedSource(FeedSource feedSource) {
        try {
            log.info("Crawling FeedSource: {} ({})", feedSource.getName(), feedSource.getUrl());

            // 1. DSL 검증
            if (feedSource.getTitleDsl() == null || feedSource.getTitleDsl().trim().isEmpty()) {
                log.error("FeedSource has no titleDsl: {}", feedSource.getId());
                return 0;
            }

            // 2. HTML 가져오기 및 파싱
            Document doc = Jsoup.connect(feedSource.getUrl())
                .timeout(10000)
                .userAgent("Mozilla/5.0")
                .get();

            // 3. 링크 목록 추출 (예: 블로그 목록 페이지의 개별 포스트 링크)
            Elements linkElements = doc.select("a[href]");
            List<String> feedUrls = linkElements.stream()
                .map(link -> link.absUrl("href"))
                .filter(url -> isValidFeedUrl(url, feedSource.getDomain()))
                .distinct()
                .collect(Collectors.toList());

            int crawledCount = 0;

            // 4. 각 링크에 대해 Feed 생성
            for (String feedUrl : feedUrls) {
                try {
                    if (feedRepository.existsByUrl(feedUrl)) {
                        log.debug("Feed already exists: {}", feedUrl);
                        continue;
                    }

                    Feed feed = crawlSingleFeed(feedUrl, feedSource);
                    if (feed != null) {
                        feedRepository.save(feed);
                        crawledCount++;
                        log.info("Feed created: {}", feed.getTitle());
                    }
                } catch (Exception e) {
                    log.error("Failed to crawl feed: {}", feedUrl, e);
                }
            }

            // 5. FeedSource 업데이트
            feedSource.setUpdatedAt(Instant.now());
            feedSourceRepository.save(feedSource);

            log.info("Crawling completed: {} feeds collected from {}",
                crawledCount, feedSource.getName());
            return crawledCount;

        } catch (Exception e) {
            log.error("Failed to crawl FeedSource: {}", feedSource.getName(), e);
            return 0;
        }
    }

    /**
     * 단일 URL을 크롤링하여 Feed 생성
     */
    private Feed crawlSingleFeed(String url, FeedSource feedSource) {
        try {
            Document doc = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0")
                .get();

            // FeedSource의 DSL을 사용하여 데이터 추출
            CrawlerDsl crawler = new CrawlerDsl(doc);
            String title = crawler.executeAsString(feedSource.getTitleDsl());
            String thumbnailUrl = feedSource.getCoverImageDsl() != null
                ? crawler.executeAsString(feedSource.getCoverImageDsl())
                : null;

            if (title == null || title.trim().isEmpty()) {
                log.warn("Title not found for URL: {}", url);
                return null;
            }

            // 발행일 추출 (메타 태그 활용)
            Instant publishedAt = extractPublishedDate(doc);

            return Feed.builder()
                .contentType(feedSource.getContentType())
                .title(title)
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .category(feedSource.getCategory())
                .tags(feedSource.getTags())
                .sourceProvider(feedSource.getDomain())
                .publishedAt(publishedAt != null ? publishedAt : Instant.now())
                .displayOrder(0)
                .viewCount(0)
                .avgReadTimeSeconds(0.0)
                .createdAt(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Failed to parse feed: {}", url, e);
            return null;
        }
    }

    private Instant extractPublishedDate(Document doc) {
        // 메타 태그에서 발행일 추출 시도
        String[] metaSelectors = {
            "meta[property=article:published_time]",
            "meta[name=publish-date]",
            "time[datetime]"
        };

        for (String selector : metaSelectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String dateStr = element.attr("content");
                if (dateStr.isEmpty()) {
                    dateStr = element.attr("datetime");
                }
                try {
                    return Instant.parse(dateStr);
                } catch (Exception e) {
                    log.debug("Failed to parse date: {}", dateStr);
                }
            }
        }
        return null;
    }

    private boolean isValidFeedUrl(String url, String domain) {
        return url != null
            && url.contains(domain)
            && (url.startsWith("http://") || url.startsWith("https://"));
    }
}
