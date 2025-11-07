package com.linglevel.api.content.feed.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Feed Description 추출 통합 테스트")
class FeedDescriptionExtractionTest {

    @Test
    @DisplayName("BBC RSS에서 description 추출 - 실제 FeedCrawlingService 메서드 호출")
    void testBbcDescriptionExtraction() throws Exception {
        // given: BBC RSS URL
        String rssUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("\n=== BBC RSS Description 추출 테스트 ===\n");

        // FeedCrawlingService의 extractDescription 메서드를 리플렉션으로 호출
        FeedCrawlingService service = new FeedCrawlingService(null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        int successCount = 0;
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            SyndEntry entry = entries.get(i);
            String description = (String) extractDescriptionMethod.invoke(service, entry);

            System.out.println("--- Entry " + (i + 1) + " ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Description: " + (description != null ? description : "(null)"));
            System.out.println();

            if (description != null && !description.trim().isEmpty()) {
                successCount++;
            }
        }

        System.out.println("✅ Description 추출 성공: " + successCount + " / " + Math.min(5, entries.size()));
        assertTrue(successCount > 0, "최소 1개 이상의 description이 추출되어야 함");
    }

    @Test
    @DisplayName("YouTube RSS에서 description 추출 - 실제 FeedCrawlingService 메서드 호출")
    void testYouTubeDescriptionExtraction() throws Exception {
        // given: YouTube RSS URL (Kurzgesagt)
        String rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("\n=== YouTube RSS Description 추출 테스트 ===\n");
        System.out.println("Channel: " + feed.getTitle());
        System.out.println();

        // FeedCrawlingService의 extractDescription 메서드를 리플렉션으로 호출
        FeedCrawlingService service = new FeedCrawlingService(null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        int successCount = 0;
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            SyndEntry entry = entries.get(i);
            String description = (String) extractDescriptionMethod.invoke(service, entry);

            System.out.println("--- Video " + (i + 1) + " ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Description (길이: " + (description != null ? description.length() : 0) + "자):");
            if (description != null) {
                System.out.println(description.length() > 200 ? description.substring(0, 200) + "..." : description);
            } else {
                System.out.println("(null)");
            }
            System.out.println();

            if (description != null && !description.trim().isEmpty()) {
                successCount++;
            }
        }

        System.out.println("✅ Description 추출 성공: " + successCount + " / " + Math.min(3, entries.size()));
        assertTrue(successCount > 0, "최소 1개 이상의 description이 추출되어야 함");
    }

    @Test
    @DisplayName("Medium RSS에서 description 추출 - 실제 FeedCrawlingService 메서드 호출")
    void testMediumDescriptionExtraction() throws Exception {
        // given: Medium RSS URL
        String rssUrl = "https://medium.com/feed/tag/programming";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        System.out.println("\n=== Medium RSS Description 추출 테스트 ===\n");

        // FeedCrawlingService의 extractDescription 메서드를 리플렉션으로 호출
        FeedCrawlingService service = new FeedCrawlingService(null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        int successCount = 0;
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            SyndEntry entry = entries.get(i);
            String description = (String) extractDescriptionMethod.invoke(service, entry);

            System.out.println("--- Article " + (i + 1) + " ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Description (길이: " + (description != null ? description.length() : 0) + "자):");
            if (description != null) {
                System.out.println(description.length() > 150 ? description.substring(0, 150) + "..." : description);
            } else {
                System.out.println("(null)");
            }
            System.out.println();

            if (description != null && !description.trim().isEmpty()) {
                successCount++;
            }
        }

        System.out.println("✅ Description 추출 성공: " + successCount + " / " + Math.min(3, entries.size()));
        assertTrue(successCount > 0, "최소 1개 이상의 description이 추출되어야 함");
    }

    @Test
    @DisplayName("통합 테스트 - 3가지 RSS 소스 모두에서 description 추출")
    void testAllSourcesDescriptionExtraction() throws Exception {
        String[][] sources = {
            {"BBC", "https://feeds.bbci.co.uk/news/technology/rss.xml"},
            {"YouTube", "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q"},
            {"Medium", "https://medium.com/feed/tag/programming"}
        };

        System.out.println("\n=== 통합 테스트: 3가지 RSS 소스 Description 추출 ===\n");

        FeedCrawlingService service = new FeedCrawlingService(null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        int totalSuccess = 0;
        int totalTested = 0;

        for (String[] source : sources) {
            String sourceName = source[0];
            String rssUrl = source[1];

            try {
                SyndFeedInput input = new SyndFeedInput();
                input.setAllowDoctypes(true);
                SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
                List<SyndEntry> entries = feed.getEntries();

                int sourceSuccess = 0;
                int testCount = Math.min(2, entries.size());

                for (int i = 0; i < testCount; i++) {
                    SyndEntry entry = entries.get(i);
                    String description = (String) extractDescriptionMethod.invoke(service, entry);
                    if (description != null && !description.trim().isEmpty()) {
                        sourceSuccess++;
                    }
                }

                totalSuccess += sourceSuccess;
                totalTested += testCount;

                System.out.println("📰 " + sourceName + ": " + sourceSuccess + " / " + testCount + " 성공");

            } catch (Exception e) {
                System.out.println("❌ " + sourceName + ": 실패 - " + e.getMessage());
            }
        }

        System.out.println("\n총합: " + totalSuccess + " / " + totalTested + " description 추출 성공");
        System.out.println("성공률: " + String.format("%.1f%%", (totalSuccess * 100.0 / totalTested)));

        assertTrue(totalSuccess > 0, "최소 1개 이상의 description이 추출되어야 함");
        assertTrue((totalSuccess * 100.0 / totalTested) >= 80.0, "80% 이상의 성공률이 필요함");
    }
}