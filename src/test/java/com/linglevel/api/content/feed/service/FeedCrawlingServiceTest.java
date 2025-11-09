package com.linglevel.api.content.feed.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RSS Feed 크롤링 테스트")
class FeedCrawlingServiceTest {

    @Test
    @DisplayName("BBC Technology RSS 피드 파싱")
    void parseBbcTechnologyRss() throws Exception {
        // given: BBC Technology RSS URL
        String rssUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertNotNull(feed.getTitle());
        assertFalse(entries.isEmpty());

        System.out.println("=== BBC Technology RSS Feed ===");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Feed Description: " + feed.getDescription());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 5개 엔트리 출력
        entries.stream().limit(5).forEach(entry -> {
            System.out.println("--- Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Published: " + entry.getPublishedDate());
            System.out.println("Updated: " + entry.getUpdatedDate());

            // Description
            if (entry.getDescription() != null) {
                String desc = entry.getDescription().getValue();
                System.out.println("Description: " + (desc.length() > 100 ? desc.substring(0, 100) + "..." : desc));
            }

            // Enclosures (썸네일)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("Enclosure: " + enc.getUrl() + " (Type: " + enc.getType() + ")");
                });
            }
            System.out.println();
        });

        // 각 엔트리 검증
        SyndEntry firstEntry = entries.get(0);
        assertNotNull(firstEntry.getTitle());
        assertNotNull(firstEntry.getLink());
        assertTrue(firstEntry.getLink().startsWith("http"));
    }

    @Test
    @DisplayName("TechCrunch RSS 피드 파싱")
    void parseTechCrunchRss() throws Exception {
        // given: TechCrunch RSS URL
        String rssUrl = "https://techcrunch.com/feed/";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertNotNull(feed.getTitle());
        assertFalse(entries.isEmpty());

        System.out.println("=== TechCrunch RSS Feed ===");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 3개 엔트리 출력
        entries.stream().limit(3).forEach(entry -> {
            System.out.println("--- Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Published: " + entry.getPublishedDate());

            // Enclosures (썸네일)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("Thumbnail: " + enc.getUrl());
                });
            }
            System.out.println();
        });

        SyndEntry firstEntry = entries.get(0);
        assertNotNull(firstEntry.getTitle());
        assertNotNull(firstEntry.getLink());
    }

    @Test
    @DisplayName("Hacker News RSS 피드 파싱")
    void parseHackerNewsRss() throws Exception {
        // given: Hacker News RSS URL
        String rssUrl = "https://news.ycombinator.com/rss";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertFalse(entries.isEmpty());

        System.out.println("=== Hacker News RSS Feed ===");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 5개 엔트리 출력
        entries.stream().limit(5).forEach(entry -> {
            System.out.println("--- Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println();
        });
    }

    @Test
    @DisplayName("The Verge RSS 피드 파싱")
    void parseTheVergeRss() throws Exception {
        // given: The Verge RSS URL
        String rssUrl = "https://www.theverge.com/rss/index.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertFalse(entries.isEmpty());

        System.out.println("=== The Verge RSS Feed ===");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 3개 엔트리 출력
        entries.stream().limit(3).forEach(entry -> {
            System.out.println("--- Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Published: " + entry.getPublishedDate());

            // Enclosures (썸네일) 확인
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("Thumbnail: " + enc.getUrl() + " (Type: " + enc.getType() + ")");
                });
            }
            System.out.println();
        });
    }

    @Test
    @DisplayName("RSS 피드 썸네일 추출 테스트")
    void extractThumbnailsFromRss() throws Exception {
        // given: 다양한 RSS 피드들
        String[] rssUrls = {
            "https://feeds.bbci.co.uk/news/technology/rss.xml",
            "https://techcrunch.com/feed/",
            "https://www.theverge.com/rss/index.xml"
        };

        System.out.println("=== RSS 피드별 썸네일 추출 테스트 ===\n");

        for (String rssUrl : rssUrls) {
            try {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
                List<SyndEntry> entries = feed.getEntries();

                System.out.println("Feed: " + feed.getTitle());
                System.out.println("URL: " + rssUrl);

                long thumbnailCount = entries.stream()
                    .filter(entry -> entry.getEnclosures() != null && !entry.getEnclosures().isEmpty())
                    .filter(entry -> entry.getEnclosures().stream()
                        .anyMatch(enc -> enc.getType() != null && enc.getType().startsWith("image/")))
                    .count();

                System.out.println("Total Entries: " + entries.size());
                System.out.println("Entries with Thumbnails: " + thumbnailCount);
                System.out.println("Thumbnail Rate: " + String.format("%.1f%%", (thumbnailCount * 100.0 / entries.size())));
                System.out.println();

            } catch (Exception e) {
                System.out.println("Failed to parse: " + rssUrl);
                System.out.println("Error: " + e.getMessage());
                System.out.println();
            }
        }
    }

    @Test
    @DisplayName("RSS 피드 발행일 포맷 확인")
    void checkPublishDateFormats() throws Exception {
        // given: BBC RSS URL
        String rssUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("=== RSS 발행일 포맷 확인 ===\n");

        entries.stream().limit(5).forEach(entry -> {
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Published Date: " + entry.getPublishedDate());
            System.out.println("Updated Date: " + entry.getUpdatedDate());

            if (entry.getPublishedDate() != null) {
                System.out.println("Published Instant: " + entry.getPublishedDate().toInstant());
            } else if (entry.getUpdatedDate() != null) {
                System.out.println("Updated Instant: " + entry.getUpdatedDate().toInstant());
            }
            System.out.println();
        });
    }

    @Test
    @DisplayName("BBC RSS description 추출 테스트")
    void extractDescriptionFromBbc() throws Exception {
        // given: BBC RSS URL
        String rssUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("=== BBC RSS Description 추출 테스트 ===\n");

        // 첫 3개 엔트리의 description 확인
        entries.stream().limit(3).forEach(entry -> {
            System.out.println("--- Entry ---");
            System.out.println("Title: " + entry.getTitle());

            // Description 확인
            if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                String description = entry.getDescription().getValue();
                String cleaned = description.replaceAll("<[^>]*>", "").trim();
                cleaned = cleaned.replaceAll("<!\\[CDATA\\[", "").replaceAll("\\]\\]>", "").trim();
                System.out.println("Description (원본): " + description);
                System.out.println("Description (정제): " + cleaned);
                assertNotNull(cleaned);
                assertFalse(cleaned.isEmpty());
            } else {
                System.out.println("Description: (없음)");
            }
            System.out.println();
        });
    }

    @Test
    @DisplayName("YouTube RSS description 추출 테스트")
    void extractDescriptionFromYouTube() throws Exception {
        // given: YouTube 채널 RSS URL (Kurzgesagt)
        String rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("=== YouTube RSS Description 추출 테스트 ===\n");
        System.out.println("Channel: " + feed.getTitle());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 3개 엔트리의 description 확인
        entries.stream().limit(3).forEach(entry -> {
            System.out.println("--- Video Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());

            // 1. 일반 Description 확인
            if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                String description = entry.getDescription().getValue();
                System.out.println("Standard Description: " +
                    (description.length() > 100 ? description.substring(0, 100) + "..." : description));
            }

            // 2. media:description 확인
            if (entry.getForeignMarkup() != null) {
                System.out.println("ForeignMarkup elements: " + entry.getForeignMarkup().size());
                for (Object element : entry.getForeignMarkup()) {
                    if (element instanceof org.jdom2.Element) {
                        org.jdom2.Element elem = (org.jdom2.Element) element;
                        System.out.println("  - Element: " + elem.getName() + " (namespace: " + elem.getNamespaceURI() + ")");

                        if ("group".equals(elem.getName())) {
                            // media:description 찾기
                            org.jdom2.Element descElem = elem.getChild("description", elem.getNamespace());
                            if (descElem != null) {
                                String mediaDesc = descElem.getText();
                                System.out.println("media:description (길이: " + mediaDesc.length() + "자): ");
                                System.out.println(mediaDesc.substring(0, Math.min(200, mediaDesc.length())) + "...");
                                assertNotNull(mediaDesc);
                                assertFalse(mediaDesc.isEmpty());
                            }
                        }
                    }
                }
            }
            System.out.println();
        });
    }

    @Test
    @DisplayName("Medium RSS description 추출 테스트")
    void extractDescriptionFromMedium() throws Exception {
        // given: Medium RSS URL (Programming tag)
        String rssUrl = "https://medium.com/feed/tag/programming";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("=== Medium RSS Description 추출 테스트 ===\n");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 첫 3개 엔트리의 description 확인
        entries.stream().limit(3).forEach(entry -> {
            System.out.println("--- Article Entry ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());

            // Description 확인
            if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                String description = entry.getDescription().getValue();
                String cleaned = description.replaceAll("<[^>]*>", "").trim();
                System.out.println("Description (정제 전 길이): " + description.length());
                System.out.println("Description (정제 후): " +
                    (cleaned.length() > 150 ? cleaned.substring(0, 150) + "..." : cleaned));
                assertNotNull(cleaned);
                assertFalse(cleaned.isEmpty());
            }

            // Content 확인
            if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                String content = entry.getContents().get(0).getValue();
                System.out.println("Content 존재: " + (content != null ? "Yes (길이: " + content.length() + ")" : "No"));
            }
            System.out.println();
        });
    }

    @Test
    @DisplayName("다양한 RSS 피드 Description 추출 종합 테스트")
    void extractDescriptionFromMultipleSources() throws Exception {
        // given: 다양한 RSS 피드들
        String[][] rssSources = {
            {"BBC", "https://feeds.bbci.co.uk/news/technology/rss.xml"},
            {"YouTube", "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q"},
            {"Medium", "https://medium.com/feed/tag/programming"}
        };

        System.out.println("=== 다양한 RSS 피드 Description 추출 종합 테스트 ===\n");

        for (String[] source : rssSources) {
            String sourceName = source[0];
            String rssUrl = source[1];

            try {
                SyndFeedInput input = new SyndFeedInput();
                input.setAllowDoctypes(true);
                SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
                List<SyndEntry> entries = feed.getEntries();

                System.out.println("📰 " + sourceName + " (" + rssUrl + ")");
                System.out.println("   Total Entries: " + entries.size());

                // description 추출 가능한 엔트리 카운트
                long withStandardDesc = entries.stream()
                    .filter(entry -> entry.getDescription() != null &&
                                   entry.getDescription().getValue() != null &&
                                   !entry.getDescription().getValue().trim().isEmpty())
                    .count();

                long withContent = entries.stream()
                    .filter(entry -> entry.getContents() != null && !entry.getContents().isEmpty())
                    .count();

                long withMediaDesc = entries.stream()
                    .filter(entry -> entry.getForeignMarkup() != null && !entry.getForeignMarkup().isEmpty())
                    .count();

                System.out.println("   With Standard Description: " + withStandardDesc +
                                 " (" + String.format("%.1f%%", withStandardDesc * 100.0 / entries.size()) + ")");
                System.out.println("   With Content: " + withContent +
                                 " (" + String.format("%.1f%%", withContent * 100.0 / entries.size()) + ")");
                System.out.println("   With Media Elements: " + withMediaDesc +
                                 " (" + String.format("%.1f%%", withMediaDesc * 100.0 / entries.size()) + ")");
                System.out.println();

            } catch (Exception e) {
                System.out.println("❌ " + sourceName + " - Failed to parse");
                System.out.println("   Error: " + e.getMessage());
                System.out.println();
            }
        }
    }
}
