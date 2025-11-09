package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.crawling.dsl.CrawlerDsl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Medium 실제 크롤링 테스트")
class MediumCrawlTest {

    @Test
    @DisplayName("Medium 글에서 실제 콘텐츠 추출")
    void crawlMediumArticle() throws Exception {
        // given
        String url = "https://medium.com/@haleemakashimri111/32-7-trending-javascript-libraries-fe427f00a3f4";
        String dsl = "D'''article p.pw-post-body-paragraph'''>#";

        System.out.println("=".repeat(80));
        System.out.println("Medium 글 크롤링 테스트");
        System.out.println("=".repeat(80));
        System.out.println("URL: " + url);
        System.out.println("DSL: " + dsl);
        System.out.println();

        // when: 페이지 가져오기
        Document doc = Jsoup.connect(url)
            .timeout(10000)
            .userAgent("Mozilla/5.0")
            .get();

        CrawlerDsl crawler = new CrawlerDsl(doc);
        String content = crawler.executeAsString(dsl);

        // then: 검증
        System.out.println("크롤링 결과:");
        System.out.println("-".repeat(80));

        if (content != null) {
            System.out.println("총 길이: " + content.length() + "자");
            System.out.println();
            System.out.println("내용 (전체):");
            System.out.println(content);
        } else {
            System.out.println("❌ 콘텐츠를 추출하지 못했습니다.");
        }

        System.out.println("-".repeat(80));
        System.out.println();

        assertNotNull(content, "콘텐츠는 null이 아니어야 함");
        assertFalse(content.trim().isEmpty(), "콘텐츠는 비어있지 않아야 함");
        assertTrue(content.length() >= 100,
            "콘텐츠는 100자 이상이어야 함 (실제: " + content.length() + "자)");

        System.out.println("✅ 테스트 통과!");
        System.out.println("   - 콘텐츠 길이: " + content.length() + "자");
        System.out.println("   - 최소 요구사항: 100자");
        System.out.println("=".repeat(80));
    }

    @Test
    @DisplayName("Medium 글 HTML 구조 분석")
    void analyzeMediumStructure() throws Exception {
        // given
        String url = "https://medium.com/@haleemakashimri111/32-7-trending-javascript-libraries-fe427f00a3f4";

        System.out.println("=".repeat(80));
        System.out.println("Medium HTML 구조 분석");
        System.out.println("=".repeat(80));
        System.out.println("URL: " + url);
        System.out.println();

        // when
        Document doc = Jsoup.connect(url)
            .timeout(10000)
            .userAgent("Mozilla/5.0")
            .get();

        System.out.println("1. article 태그 개수: " + doc.select("article").size());
        System.out.println("2. p.pw-post-body-paragraph 개수: " +
            doc.select("p.pw-post-body-paragraph").size());
        System.out.println("3. article p.pw-post-body-paragraph 개수: " +
            doc.select("article p.pw-post-body-paragraph").size());
        System.out.println();

        System.out.println("첫 3개 문단:");
        System.out.println("-".repeat(80));

        var paragraphs = doc.select("article p.pw-post-body-paragraph");
        for (int i = 0; i < Math.min(3, paragraphs.size()); i++) {
            String text = paragraphs.get(i).text();
            System.out.println((i + 1) + ". " + text);
            System.out.println();
        }

        System.out.println("=".repeat(80));
    }
}