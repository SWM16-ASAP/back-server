package com.linglevel.api.content.feed.service;

import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedRepository;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.crawling.dsl.CrawlerDsl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedCrawlingService {

    private final FeedRepository feedRepository;
    private final FeedSourceRepository feedSourceRepository;

    /**
     * RSS FeedSource를 파싱하여 Feed 생성/업데이트
     *
     * @param feedSource RSS Feed 소스
     * @return 수집된 Feed 개수
     */
    public int crawlFeedSource(FeedSource feedSource) {
        try {
            log.info("Crawling RSS FeedSource: {} ({})", feedSource.getName(), feedSource.getUrl());

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed rssFeed = input.build(new XmlReader(new URL(feedSource.getUrl())));

            List<SyndEntry> entries = rssFeed.getEntries();
            log.info("Found {} entries in RSS feed: {}", entries.size(), feedSource.getName());

            int crawledCount = 0;

            for (SyndEntry entry : entries) {
                try {
                    String feedUrl = entry.getLink();

                    if (feedRepository.existsByUrl(feedUrl)) {
                        log.debug("Feed already exists: {}", feedUrl);
                        continue;
                    }

                    Feed feed = convertEntryToFeed(entry, feedSource);
                    if (feed != null) {
                        feedRepository.save(feed);
                        crawledCount++;
                        log.info("Feed created: {}", feed.getTitle());
                    }
                } catch (Exception e) {
                    log.error("Failed to convert RSS entry to Feed: {}", entry.getLink(), e);
                }
            }

            feedSource.setUpdatedAt(Instant.now());
            feedSourceRepository.save(feedSource);

            log.info("RSS crawling completed: {} feeds collected from {}",
                crawledCount, feedSource.getName());
            return crawledCount;

        } catch (Exception e) {
            log.error("Failed to crawl RSS FeedSource: {}", feedSource.getName(), e);
            return 0;
        }
    }

    /**
     * RSS Entry를 Feed 엔티티로 변환
     */
    private Feed convertEntryToFeed(SyndEntry entry, FeedSource feedSource) {
        try {
            String title = entry.getTitle();
            String url = entry.getLink();

            if (title == null || title.trim().isEmpty() || url == null) {
                log.warn("RSS entry missing title or link");
                return null;
            }

            // 썸네일 URL 추출 (RSS -> DSL fallback)
            String thumbnailUrl = extractThumbnailUrl(entry, url, feedSource);

            // 발행일 추출
            Instant publishedAt = extractPublishedDate(entry);

            return Feed.builder()
                .contentType(feedSource.getContentType())
                .title(title.trim())
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
            log.error("Failed to convert entry: {}", entry.getLink(), e);
            return null;
        }
    }

    /**
     * RSS Entry에서 썸네일 URL 추출 (RSS -> DSL fallback)
     * 1. RSS enclosures에서 이미지 찾기
     * 2. 실패하면 coverImageDsl을 사용하여 article 페이지에서 크롤링
     */
    private String extractThumbnailUrl(SyndEntry entry, String articleUrl, FeedSource feedSource) {
        // 1. Enclosures에서 이미지 찾기
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            String rssThumbnail = entry.getEnclosures().stream()
                .filter(enc -> enc.getType() != null && enc.getType().startsWith("image/"))
                .map(enc -> enc.getUrl())
                .findFirst()
                .orElse(null);

            if (rssThumbnail != null) {
                log.debug("Thumbnail found in RSS enclosures: {}", rssThumbnail);
                return rssThumbnail;
            }
        }

        // 2. RSS에 썸네일이 없고, coverImageDsl이 설정되어 있으면 크롤링
        if (feedSource.getCoverImageDsl() != null && !feedSource.getCoverImageDsl().trim().isEmpty()) {
            try {
                log.debug("RSS thumbnail not found, crawling article: {}", articleUrl);
                Document doc = Jsoup.connect(articleUrl)
                    .timeout(10000)
                    .userAgent("Mozilla/5.0")
                    .get();

                CrawlerDsl crawler = new CrawlerDsl(doc);
                String crawledThumbnail = crawler.executeAsString(feedSource.getCoverImageDsl());

                if (crawledThumbnail != null && !crawledThumbnail.trim().isEmpty()) {
                    log.debug("Thumbnail found via DSL: {}", crawledThumbnail);
                    return crawledThumbnail.trim();
                }
            } catch (Exception e) {
                log.warn("Failed to crawl thumbnail from article: {}", articleUrl, e);
            }
        }

        return null;
    }

    /**
     * RSS Entry에서 발행일 추출
     */
    private Instant extractPublishedDate(SyndEntry entry) {
        Date publishedDate = entry.getPublishedDate();
        if (publishedDate != null) {
            return publishedDate.toInstant();
        }

        Date updatedDate = entry.getUpdatedDate();
        if (updatedDate != null) {
            return updatedDate.toInstant();
        }

        return null;
    }
}
