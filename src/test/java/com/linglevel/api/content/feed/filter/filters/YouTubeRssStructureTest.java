package com.linglevel.api.content.feed.filter.filters;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jdom2.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

@DisplayName("YouTube RSS 구조 분석 테스트")
class YouTubeRssStructureTest {

    @Test
    @DisplayName("YouTube RSS 피드의 media 네임스페이스 구조 분석")
    void analyzeYouTubeRssStructure() throws Exception {
        // given: Kurzgesagt YouTube 채널 RSS 피드
        String youtubeRssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q";

        System.out.println("===========================================");
        System.out.println("=== YouTube RSS Structure Analysis ===");
        System.out.println("===========================================");
        System.out.println("YouTube RSS URL: " + youtubeRssUrl);
        System.out.println();

        // when: RSS 피드 파싱
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(youtubeRssUrl)));
        List<SyndEntry> entries = feed.getEntries();

        if (entries.isEmpty()) {
            System.out.println("No entries found!");
            return;
        }

        // 첫 번째 엔트리 상세 분석
        SyndEntry firstEntry = entries.get(0);

        System.out.println("=== First Entry Details ===");
        System.out.println("Title: " + firstEntry.getTitle());
        System.out.println("Link: " + firstEntry.getLink());
        System.out.println();

        System.out.println("=== Foreign Markup Analysis ===");
        if (firstEntry.getForeignMarkup() == null) {
            System.out.println("No foreign markup found!");
            return;
        }

        int elementCount = 0;
        for (Object element : firstEntry.getForeignMarkup()) {
            elementCount++;
            System.out.println("\n--- Element #" + elementCount + " ---");

            if (element instanceof Element) {
                Element elem = (Element) element;
                System.out.println("Element Name: " + elem.getName());
                System.out.println("Namespace URI: " + elem.getNamespaceURI());
                System.out.println("Namespace Prefix: " + elem.getNamespacePrefix());

                // media:group인 경우
                if ("group".equals(elem.getName())) {
                    System.out.println("\n*** Found media:group ***");
                    analyzeMediaGroup(elem);
                }
            } else {
                System.out.println("Element type: " + element.getClass().getName());
                System.out.println("Element: " + element);
            }
        }

        System.out.println("\n===========================================");
    }

    private void analyzeMediaGroup(Element mediaGroup) {
        System.out.println("Children of media:group:");

        List<Element> children = mediaGroup.getChildren();
        for (Element child : children) {
            System.out.println("\n  - Child: " + child.getName());
            System.out.println("    Namespace: " + child.getNamespaceURI());

            // Attributes
            if (!child.getAttributes().isEmpty()) {
                System.out.println("    Attributes:");
                child.getAttributes().forEach(attr -> {
                    System.out.println("      " + attr.getName() + " = " + attr.getValue());
                });
            }

            // Text content
            String text = child.getTextTrim();
            if (!text.isEmpty()) {
                System.out.println("    Text: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text));
            }

            // media:content 특별 처리
            if ("content".equals(child.getName())) {
                System.out.println("\n    *** Found media:content ***");
                System.out.println("    All attributes:");
                child.getAttributes().forEach(attr -> {
                    System.out.println("      - " + attr.getName() + " = " + attr.getValue());
                });
            }
        }
    }
}
