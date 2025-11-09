package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("YouTubeShortsFilter 테스트")
class YouTubeShortsFilterTest {

    private YouTubeShortsFilter filter;

    @BeforeEach
    void setUp() {
        filter = new YouTubeShortsFilter();
    }

    @Test
    @DisplayName("실제 YouTube RSS 피드에서 duration 추출 및 Shorts 필터링 테스트")
    void testRealYouTubeFeedWithDuration() throws Exception {
        // given: Kurzgesagt YouTube 채널 RSS 피드
        String youtubeRssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q";

        System.out.println("===========================================");
        System.out.println("=== YouTube Shorts Filter Test ===");
        System.out.println("===========================================");
        System.out.println("YouTube RSS URL: " + youtubeRssUrl);
        System.out.println();

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(youtubeRssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        assertNotNull(entries, "RSS entries should not be null");
        assertFalse(entries.isEmpty(), "RSS entries should not be empty");

        System.out.println("Total entries: " + entries.size());
        System.out.println();

        FeedSource mockFeedSource = mock(FeedSource.class);

        // then: 각 엔트리에 대해 필터 테스트
        int totalEntries = 0;
        int passedEntries = 0;
        int filteredEntries = 0;

        for (SyndEntry entry : entries) {
            totalEntries++;
            String title = entry.getTitle();
            String url = entry.getLink();

            // Duration 추출 (디버깅용)
            Integer duration = extractDuration(entry);

            FeedFilterResult result = filter.filter(entry, mockFeedSource);

            System.out.println("-------------------------------------------");
            System.out.println("Entry #" + totalEntries);
            System.out.println("Title: " + title);
            System.out.println("URL: " + url);
            System.out.println("Duration: " + (duration != null ? duration + "s" : "N/A"));
            System.out.println("Result: " + (result.isPassed() ? "✅ PASS" : "❌ FILTERED"));
            if (!result.isPassed()) {
                System.out.println("Reason: " + result.getReason());
                filteredEntries++;
            } else {
                passedEntries++;
            }

            // Duration이 60초 이하인 경우 필터링되어야 함
            if (duration != null && duration <= 60) {
                assertFalse(result.isPassed(),
                    "Duration이 60초 이하인 영상은 필터링되어야 함: " + title);
                assertEquals("YouTubeShortsFilter", result.getFilterName());
                assertTrue(result.getReason().contains("YouTube Shorts"));
            }
        }

        System.out.println("-------------------------------------------");
        System.out.println();
        System.out.println("=== Test Summary ===");
        System.out.println("Total entries: " + totalEntries);
        System.out.println("Passed entries: " + passedEntries);
        System.out.println("Filtered entries (Shorts): " + filteredEntries);
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("URL이 null이면 통과")
    void testNullUrl() {
        // given: URL이 null
        SyndEntry entry = mock(SyndEntry.class);
        when(entry.getLink()).thenReturn(null);

        FeedSource feedSource = mock(FeedSource.class);

        // when: 필터 실행
        FeedFilterResult result = filter.filter(entry, feedSource);

        // then: 통과해야 함
        assertTrue(result.isPassed(), "URL이 null이면 통과");
    }

    @Test
    @DisplayName("ForeignMarkup이 없으면 통과")
    void testNoForeignMarkup() {
        // given: ForeignMarkup이 null
        SyndEntry entry = mock(SyndEntry.class);
        when(entry.getLink()).thenReturn("https://youtube.com/watch?v=test");
        when(entry.getForeignMarkup()).thenReturn(null);

        FeedSource feedSource = mock(FeedSource.class);

        // when: 필터 실행
        FeedFilterResult result = filter.filter(entry, feedSource);

        // then: 통과해야 함 (duration 정보 없음)
        assertTrue(result.isPassed(), "ForeignMarkup이 없으면 duration을 확인할 수 없으므로 통과");
    }

    @Test
    @DisplayName("YouTube가 아닌 URL은 통과")
    void testNonYouTubeUrl() {
        // given: YouTube가 아닌 URL
        SyndEntry entry = mock(SyndEntry.class);
        when(entry.getLink()).thenReturn("https://example.com/shorts/test");

        FeedSource feedSource = mock(FeedSource.class);

        // when: 필터 실행
        FeedFilterResult result = filter.filter(entry, feedSource);

        // then: 통과해야 함 (YouTube 필터는 YouTube에만 적용)
        assertTrue(result.isPassed(), "YouTube가 아닌 URL은 필터 대상이 아님");
    }

    @Test
    @DisplayName("필터 이름 확인")
    void testFilterName() {
        // when
        String filterName = filter.getName();

        // then
        assertEquals("YouTubeShortsFilter", filterName);
    }

    @Test
    @DisplayName("필터 순서 확인 - URL 체크이므로 우선순위 높음")
    void testFilterOrder() {
        // when
        int order = filter.getOrder();

        // then
        assertEquals(10, order, "빠른 체크이므로 우선순위가 높아야 함");
    }

    // Helper method to extract duration for debugging
    private Integer extractDuration(SyndEntry entry) {
        if (entry.getForeignMarkup() == null) {
            return null;
        }

        for (Object element : entry.getForeignMarkup()) {
            if (element instanceof org.jdom2.Element) {
                org.jdom2.Element elem = (org.jdom2.Element) element;

                if ("group".equals(elem.getName()) &&
                    elem.getNamespaceURI() != null &&
                    (elem.getNamespaceURI().contains("media") ||
                     elem.getNamespaceURI().contains("mrss"))) {

                    org.jdom2.Element content = elem.getChild("content", elem.getNamespace());
                    if (content != null) {
                        String durationStr = content.getAttributeValue("duration");
                        if (durationStr != null) {
                            try {
                                return Integer.parseInt(durationStr);
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
