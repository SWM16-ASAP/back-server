package com.linglevel.api.content.feed.service;

import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedRepository;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.content.feed.filter.FeedFilterChain;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
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
import org.springframework.web.util.HtmlUtils;

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
    private final FeedFilterChain feedFilterChain;

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
            input.setAllowDoctypes(true);
            SyndFeed rssFeed = input.build(new XmlReader(new URL(feedSource.getUrl())));

            List<SyndEntry> entries = rssFeed.getEntries();
            log.info("Found {} entries in RSS feed: {}", entries.size(), feedSource.getName());

            int crawledCount = 0;
            int filteredCount = 0;

            for (SyndEntry entry : entries) {
                try {
                    String feedUrl = entry.getLink();

                    if (feedRepository.existsByUrl(feedUrl)) {
                        log.debug("Feed already exists: {}", feedUrl);
                        continue;
                    }

                    // 필터링 체크
                    FeedFilterResult filterResult = feedFilterChain.executeFilters(entry, feedSource);

                    Feed feed = convertEntryToFeed(entry, feedSource);
                    if (feed != null) {
                        if (!filterResult.isPassed()) {
                            feed.setDeleted(true);
                            feed.setDeletedAt(Instant.now());
                            filteredCount++;
                        }

                        feedRepository.save(feed);
                        crawledCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to convert RSS entry to Feed: {}", entry.getLink(), e);
                }
            }

            feedSource.setUpdatedAt(Instant.now());
            feedSourceRepository.save(feedSource);

            log.info("RSS crawling completed: {} feeds collected, {} filtered (soft-deleted) from {}",
                crawledCount, filteredCount, feedSource.getName());
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

            // 작성자 추출
            String author = entry.getAuthor();

            // 설명 추출
            String description = extractDescription(entry);

            return Feed.builder()
                .contentType(feedSource.getContentType())
                .title(title.trim())
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .author(author != null && !author.trim().isEmpty() ? author.trim() : null)
                .description(description)
                .category(feedSource.getCategory())
                .tags(feedSource.getTags())
                .sourceProvider(extractDomainFromUrl(url))
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
     * 2. Media 모듈에서 썸네일 찾기 (YouTube 등)
     * 3. 실패하면 coverImageDsl을 사용하여 article 페이지에서 크롤링
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

        // 2. Media 모듈에서 썸네일 찾기 (YouTube의 media:thumbnail)
        try {
            if (entry.getForeignMarkup() != null) {
                log.debug("ForeignMarkup size: {}", entry.getForeignMarkup().size());
                for (Object element : entry.getForeignMarkup()) {
                    if (element instanceof org.jdom2.Element) {
                        org.jdom2.Element elem = (org.jdom2.Element) element;
                        log.debug("Element name: {}, namespace: {}, namespaceURI: {}",
                            elem.getName(), elem.getNamespace(), elem.getNamespaceURI());

                        // media:group > media:thumbnail 태그 찾기
                        if ("group".equals(elem.getName()) && elem.getNamespaceURI() != null &&
                            (elem.getNamespaceURI().contains("media") || elem.getNamespaceURI().contains("mrss"))) {

                            log.debug("Found media:group, searching for thumbnail...");

                            // 방법 1: 같은 namespace로 찾기
                            org.jdom2.Element thumbnail = elem.getChild("thumbnail", elem.getNamespace());
                            if (thumbnail != null && thumbnail.getAttributeValue("url") != null) {
                                String thumbnailUrl = thumbnail.getAttributeValue("url");
                                log.info("Thumbnail found in media module: {}", thumbnailUrl);
                                return thumbnailUrl;
                            }

                            // 방법 2: 모든 자식 요소 탐색
                            for (Object child : elem.getChildren()) {
                                if (child instanceof org.jdom2.Element) {
                                    org.jdom2.Element childElem = (org.jdom2.Element) child;
                                    if ("thumbnail".equals(childElem.getName())) {
                                        String thumbnailUrl = childElem.getAttributeValue("url");
                                        if (thumbnailUrl != null) {
                                            log.info("Thumbnail found via children search: {}", thumbnailUrl);
                                            return thumbnailUrl;
                                        }
                                    }
                                }
                            }

                            log.debug("media:group found but no thumbnail child");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract thumbnail from media module", e);
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
     * RSS Entry에서 설명 추출
     * 1. RSS description 필드 (일반 RSS)
     * 2. media:description (YouTube 등)
     * 3. contents 필드
     */
    private String extractDescription(SyndEntry entry) {
        // 1. description 필드에서 추출 (일반 RSS: BBC, Medium 등)
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            String description = entry.getDescription().getValue();

            // Medium의 경우 medium-feed-snippet 클래스의 내용만 추출
            if (description.contains("medium-feed-snippet")) {
                int snippetStart = description.indexOf("<p class=\"medium-feed-snippet\">");
                if (snippetStart != -1) {
                    snippetStart += "<p class=\"medium-feed-snippet\">".length();
                    int snippetEnd = description.indexOf("</p>", snippetStart);
                    if (snippetEnd != -1) {
                        description = description.substring(snippetStart, snippetEnd);
                    }
                }
            }

            // HTML 태그 제거
            description = description.replaceAll("<[^>]*>", "").trim();
            // CDATA, 엔티티 정리
            description = description.replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "").trim();
            // HTML 엔티티 디코딩 (&#x2026; -> …)
            description = decodeHtmlEntities(description);
            // 개행문자 띄어쓰기로 변경
            description = description.replaceAll("[\\n\\r]", " ").trim();

            if (!description.isEmpty()) {
                // 너무 긴 경우 일부만 추출 (500자 제한)
                return description.length() > 500 ? description.substring(0, 500) + "..." : description;
            }
        }

        // 2. media:description에서 추출 (YouTube 등)
        try {
            if (entry.getForeignMarkup() != null) {
                for (Object element : entry.getForeignMarkup()) {
                    if (element instanceof org.jdom2.Element) {
                        org.jdom2.Element elem = (org.jdom2.Element) element;

                        // media:group > media:description 태그 찾기
                        if ("group".equals(elem.getName()) && elem.getNamespaceURI() != null &&
                            (elem.getNamespaceURI().contains("media") || elem.getNamespaceURI().contains("mrss"))) {

                            // 같은 namespace로 찾기
                            org.jdom2.Element descriptionElem = elem.getChild("description", elem.getNamespace());
                            if (descriptionElem != null && descriptionElem.getText() != null) {
                                String description = descriptionElem.getText().trim();
                                if (!description.isEmpty()) {
                                    log.debug("Description found in media module: {}", description.substring(0, Math.min(50, description.length())));
                                    description = description.replaceAll("[\\n\\r]", " ").trim();
                                    // 너무 긴 경우 일부만 추출 (500자 제한)
                                    return description.length() > 500 ? description.substring(0, 500) + "..." : description;
                                }
                            }

                            // 모든 자식 요소 탐색
                            for (Object child : elem.getChildren()) {
                                if (child instanceof org.jdom2.Element) {
                                    org.jdom2.Element childElem = (org.jdom2.Element) child;
                                    if ("description".equals(childElem.getName())) {
                                        String description = childElem.getText();
                                        if (description != null && !description.trim().isEmpty()) {
                                            description = description.trim();
                                            log.debug("Description found via children search: {}", description.substring(0, Math.min(50, description.length())));
                                            description = description.replaceAll("[\\n\\r]", " ").trim();
                                            // 너무 긴 경우 일부만 추출 (500자 제한)
                                            return description.length() > 500 ? description.substring(0, 500) + "..." : description;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract description from media module", e);
        }

        // 3. contents에서 추출
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            String content = entry.getContents().get(0).getValue();
            if (content != null) {
                // HTML 태그 제거
                content = content.replaceAll("<[^>]*>", "").trim();
                if (!content.isEmpty()) {
                    content = content.replaceAll("[\\n\\r]", " ").trim();
                    // 너무 긴 경우 일부만 추출 (500자 제한)
                    return content.length() > 500 ? content.substring(0, 500) + "..." : content;
                }
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

    /**
     * URL에서 도메인 추출
     */
    private String extractDomainFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            log.warn("Failed to extract domain from URL: {}", urlString, e);
            return null;
        }
    }

    private String decodeHtmlEntities(String text) {
        return HtmlUtils.htmlUnescape(text);
    }
}
