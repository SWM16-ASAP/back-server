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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("새로운 RSS Feed 소스 파싱 테스트 (Formula1, ESPN)")
class NewFeedSourcesTest {

    @Test
    @DisplayName("Formula1 RSS 피드 파싱 테스트")
    void testFormula1RssFeed() throws Exception {
        // given: Formula1 RSS URL
        String rssUrl = "https://www.formula1.com/en/latest/all.xml";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertNotNull(feed.getTitle());
        assertFalse(entries.isEmpty(), "Formula1 RSS 피드에 엔트리가 있어야 함");

        System.out.println("\n=== Formula1 RSS Feed 파싱 결과 ===\n");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Feed Description: " + feed.getDescription());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // FeedCrawlingService의 extractDescription 메서드를 리플렉션으로 호출
        FeedCrawlingService service = new FeedCrawlingService(null, null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        // 첫 5개 엔트리 상세 출력
        int successCount = 0;
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            SyndEntry entry = entries.get(i);

            System.out.println("--- Entry " + (i + 1) + " ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Published: " + entry.getPublishedDate());
            System.out.println("Author: " + entry.getAuthor());

            // Description 추출 테스트
            String description = (String) extractDescriptionMethod.invoke(service, entry);
            if (description != null && !description.trim().isEmpty()) {
                System.out.println("Description (길이: " + description.length() + "자): ");
                System.out.println(description.length() > 200 ? description.substring(0, 200) + "..." : description);
                successCount++;
            } else {
                System.out.println("Description: (없음)");
            }

            // Enclosures 확인 (썸네일)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                System.out.println("Enclosures:");
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("  - URL: " + enc.getUrl());
                    System.out.println("    Type: " + enc.getType());
                });
            }

            // ForeignMarkup 확인 (media:* 태그)
            if (entry.getForeignMarkup() != null && !entry.getForeignMarkup().isEmpty()) {
                System.out.println("ForeignMarkup elements: " + entry.getForeignMarkup().size());
                for (Object element : entry.getForeignMarkup()) {
                    if (element instanceof org.jdom2.Element) {
                        org.jdom2.Element elem = (org.jdom2.Element) element;
                        System.out.println("  - Element: " + elem.getName() + " (namespace: " + elem.getNamespaceURI() + ")");
                    }
                }
            }

            System.out.println();
        }

        System.out.println("✅ Description 추출 성공: " + successCount + " / " + Math.min(5, entries.size()));

        // 각 엔트리 검증
        SyndEntry firstEntry = entries.get(0);
        assertNotNull(firstEntry.getTitle(), "Title이 있어야 함");
        assertNotNull(firstEntry.getLink(), "Link가 있어야 함");
        assertTrue(firstEntry.getLink().startsWith("http"), "Link는 http로 시작해야 함");
    }

    @Test
    @DisplayName("ESPN MLB RSS 피드 파싱 테스트")
    void testEspnMlbRssFeed() throws Exception {
        // given: ESPN MLB RSS URL
        String rssUrl = "https://www.espn.com/espn/rss/mlb/news";

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        // then: 피드 정보 확인
        assertNotNull(feed);
        assertNotNull(feed.getTitle());
        assertFalse(entries.isEmpty(), "ESPN MLB RSS 피드에 엔트리가 있어야 함");

        System.out.println("\n=== ESPN MLB RSS Feed 파싱 결과 ===\n");
        System.out.println("Feed Title: " + feed.getTitle());
        System.out.println("Feed Description: " + feed.getDescription());
        System.out.println("Total Entries: " + entries.size());
        System.out.println();

        // FeedCrawlingService의 extractDescription 메서드를 리플렉션으로 호출
        FeedCrawlingService service = new FeedCrawlingService(null, null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        // 첫 5개 엔트리 상세 출력
        int successCount = 0;
        for (int i = 0; i < Math.min(5, entries.size()); i++) {
            SyndEntry entry = entries.get(i);

            System.out.println("--- Entry " + (i + 1) + " ---");
            System.out.println("Title: " + entry.getTitle());
            System.out.println("Link: " + entry.getLink());
            System.out.println("Published: " + entry.getPublishedDate());
            System.out.println("Author: " + entry.getAuthor());

            // Description 추출 테스트
            String description = (String) extractDescriptionMethod.invoke(service, entry);
            if (description != null && !description.trim().isEmpty()) {
                System.out.println("Description (길이: " + description.length() + "자): ");
                System.out.println(description.length() > 200 ? description.substring(0, 200) + "..." : description);
                successCount++;
            } else {
                System.out.println("Description: (없음)");
            }

            // Enclosures 확인 (썸네일)
            if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                System.out.println("Enclosures:");
                entry.getEnclosures().forEach(enc -> {
                    System.out.println("  - URL: " + enc.getUrl());
                    System.out.println("    Type: " + enc.getType());
                });
            }

            // ForeignMarkup 확인 (media:* 태그)
            if (entry.getForeignMarkup() != null && !entry.getForeignMarkup().isEmpty()) {
                System.out.println("ForeignMarkup elements: " + entry.getForeignMarkup().size());
                for (Object element : entry.getForeignMarkup()) {
                    if (element instanceof org.jdom2.Element) {
                        org.jdom2.Element elem = (org.jdom2.Element) element;
                        System.out.println("  - Element: " + elem.getName() + " (namespace: " + elem.getNamespaceURI() + ")");
                    }
                }
            }

            System.out.println();
        }

        System.out.println("✅ Description 추출 성공: " + successCount + " / " + Math.min(5, entries.size()));

        // 각 엔트리 검증
        SyndEntry firstEntry = entries.get(0);
        assertNotNull(firstEntry.getTitle(), "Title이 있어야 함");
        assertNotNull(firstEntry.getLink(), "Link가 있어야 함");
        assertTrue(firstEntry.getLink().startsWith("http"), "Link는 http로 시작해야 함");
    }

    @Test
    @DisplayName("Formula1 + ESPN MLB RSS 통합 비교 테스트")
    void testBothNewSourcesComparison() throws Exception {
        String[][] sources = {
            {"Formula1", "https://www.formula1.com/en/latest/all.xml"},
            {"ESPN MLB", "https://www.espn.com/espn/rss/mlb/news"}
        };

        System.out.println("\n=== 새로운 RSS 소스 통합 비교 테스트 ===\n");

        FeedCrawlingService service = new FeedCrawlingService(null, null, null);
        Method extractDescriptionMethod = FeedCrawlingService.class.getDeclaredMethod("extractDescription", SyndEntry.class);
        extractDescriptionMethod.setAccessible(true);

        for (String[] source : sources) {
            String sourceName = source[0];
            String rssUrl = source[1];

            try {
                SyndFeedInput input = new SyndFeedInput();
                input.setAllowDoctypes(true);
                SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
                List<SyndEntry> entries = feed.getEntries();

                System.out.println("📰 " + sourceName + " (" + rssUrl + ")");
                System.out.println("   Feed Title: " + feed.getTitle());
                System.out.println("   Total Entries: " + entries.size());

                // description 추출 가능한 엔트리 카운트
                int descriptionCount = 0;
                int thumbnailCount = 0;

                for (SyndEntry entry : entries) {
                    // Description 체크
                    String description = (String) extractDescriptionMethod.invoke(service, entry);
                    if (description != null && !description.trim().isEmpty()) {
                        descriptionCount++;
                    }

                    // Thumbnail 체크
                    if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
                        boolean hasImageEnclosure = entry.getEnclosures().stream()
                            .anyMatch(enc -> enc.getType() != null && enc.getType().startsWith("image/"));
                        if (hasImageEnclosure) {
                            thumbnailCount++;
                        }
                    }
                }

                System.out.println("   With Description: " + descriptionCount +
                                 " (" + String.format("%.1f%%", descriptionCount * 100.0 / entries.size()) + ")");
                System.out.println("   With Thumbnail: " + thumbnailCount +
                                 " (" + String.format("%.1f%%", thumbnailCount * 100.0 / entries.size()) + ")");
                System.out.println();

                assertTrue(descriptionCount > 0 || thumbnailCount > 0,
                    sourceName + "에서 최소한 description 또는 thumbnail 중 하나는 추출되어야 함");

            } catch (Exception e) {
                System.out.println("❌ " + sourceName + " - Failed to parse");
                System.out.println("   Error: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
                fail(sourceName + " RSS 파싱 실패: " + e.getMessage());
            }
        }
    }
}