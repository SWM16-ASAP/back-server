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

@DisplayName("Medium RSS 피드 테스트")
class MediumRssTest {

    @Test
    @DisplayName("Medium Programming 태그 RSS 피드 파싱")
    void parseMediumProgrammingRss() throws Exception {
        // given: Medium Programming RSS URL
        String rssUrl = "https://medium.com/feed/tag/programming";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertNotNull(feed.getTitle());
        assertFalse(entries.isEmpty());

        System.out.println("===========================================");
        System.out.println("=== Medium Programming RSS Feed ===");
        System.out.println("===========================================");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Feed Link: " + feed.getLink());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // 모든 엔트리 출력
        System.out.println("=== All Entries ===\n");
        for (int i = 0; i < entries.size(); i++) {
            SyndEntry entry = entries.get(i);
            System.out.println("=== Entry #" + (i + 1) + " ===");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Author: " + (entry.getAuthor() != null ? entry.getAuthor() : "N/A"));
            System.out.println("Published: " + entry.getPublishedDate());
            System.out.println("Updated: " + entry.getUpdatedDate());

            // Description
            if (entry.getDescription() != null) {
                String desc = entry.getDescription().getValue();
                String cleanDesc = desc.replaceAll("<[^>]*>", "").trim();
                System.out.println("Description: " + (cleanDesc.length() > 150 ? cleanDesc.substring(0, 150) + "..." : cleanDesc));
            }

            // Categories/Tags
            if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
                System.out.print("Categories: ");
                entry.getCategories().forEach(cat -> System.out.print(cat.getName() + ", "));
                System.out.println();
            }

            // Enclosures (썸네일)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                System.out.println("Thumbnails:");
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("  - " + enc.getUrl() + " (Type: " + enc.getType() + ")");
                });
            } else {
                System.out.println("Thumbnails: None");
            }

            System.out.println();
        }

        // 썸네일 통계
        long thumbnailCount = entries.stream()
            .filter(entry -> entry.getEnclosures() != null && !entry.getEnclosures().isEmpty())
            .count();

        System.out.println("===========================================");
        System.out.println("=== Summary ===");
        System.out.println("===========================================");
        System.out.println("Total Entries: " + entries.size());
        System.out.println("Entries with Thumbnails: " + thumbnailCount);
        System.out.println("Thumbnail Rate: " + String.format("%.1f%%", (thumbnailCount * 100.0 / entries.size())));
        System.out.println("===========================================");

        // 검증
        SyndEntry firstEntry = entries.get(0);
        assertNotNull(firstEntry.getTitle());
        assertNotNull(firstEntry.getLink());
        assertTrue(firstEntry.getLink().startsWith("http"));
    }

    @Test
    @DisplayName("Medium 다양한 태그 RSS 피드 비교")
    void compareMediumTagFeeds() throws Exception {
        // given: 다양한 Medium 태그 RSS URLs
        String[] rssUrls = {
            "https://medium.com/feed/tag/programming",
            "https://medium.com/feed/tag/javascript",
            "https://medium.com/feed/tag/technology"
        };

        System.out.println("===========================================");
        System.out.println("=== Medium 태그별 RSS 피드 비교 ===");
        System.out.println("===========================================\n");

        for (String rssUrl : rssUrls) {
            try {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
                List<SyndEntry> entries = feed.getEntries();

                System.out.println("--- " + feed.getTitle() + " ---");
                System.out.println("URL: " + rssUrl);
                System.out.println("Total Entries: " + entries.size());

                long thumbnailCount = entries.stream()
                    .filter(entry -> entry.getEnclosures() != null && !entry.getEnclosures().isEmpty())
                    .count();

                System.out.println("Entries with Thumbnails: " + thumbnailCount);
                System.out.println("Thumbnail Rate: " + String.format("%.1f%%", (thumbnailCount * 100.0 / entries.size())));

                // 첫 3개 타이틀 출력
                System.out.println("\nTop 3 Articles:");
                entries.stream().limit(3).forEach(entry -> {
                    System.out.println("  - " + entry.getTitle());
                });

                System.out.println("\n");

            } catch (Exception e) {
                System.out.println("Failed to parse: " + rssUrl);
                System.out.println("Error: " + e.getMessage());
                System.out.println();
            }
        }
    }
}
