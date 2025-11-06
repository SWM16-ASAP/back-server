package com.linglevel.api.crawling.dsl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrawlerDslTest {

    @Test
    void testBasicTextExtraction() {
        String html = """
            <html>
                <head>
                    <title>Test Page</title>
                </head>
                <body>
                    <h1 id="main-title">Welcome to Test</h1>
                    <div class="content">
                        <p>First paragraph</p>
                        <p>Second paragraph</p>
                    </div>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Test 1: Extract title text
        String result = crawler.executeAsString("D'title'#");
        assertEquals("Test Page", result);

        // Test 2: Extract h1 text
        String h1Text = crawler.executeAsString("D'h1#main-title'#");
        assertEquals("Welcome to Test", h1Text);

        // Test 3: Extract all paragraphs
        String paragraphs = crawler.executeAsString("D'''p'''>#");
        assertNotNull(paragraphs);
        assertTrue(paragraphs.contains("First paragraph"));
        assertTrue(paragraphs.contains("Second paragraph"));
    }

    @Test
    void testAttributeExtraction() {
        String html = """
            <html>
                <body>
                    <a href="https://example.com" class="link">Example Link</a>
                    <img src="image.jpg" alt="Test Image">
                    <meta property="og:title" content="Open Graph Title">
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Test 1: Extract href attribute
        String href = crawler.executeAsString("D'a.link'@'href'");
        assertEquals("https://example.com", href);

        // Test 2: Extract img src
        String src = crawler.executeAsString("D'img'@'src'");
        assertEquals("image.jpg", src);

        // Test 3: Extract meta content
        String ogTitle = crawler.executeAsString("D'meta[property=\"og:title\"]'@'content'");
        assertEquals("Open Graph Title", ogTitle);
    }

    @Test
    void testNullCoalescing() {
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="Fallback Title">
                </head>
                <body>
                    <h1 class="title">Main Title</h1>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Test 1: First option exists
        String result1 = crawler.executeAsString("D'h1.title'# ? D'meta[property=\"og:title\"]'@'content'");
        assertEquals("Main Title", result1);

        // Test 2: First option doesn't exist, fallback to second
        String result2 = crawler.executeAsString("D'h1.nonexistent'# ? D'meta[property=\"og:title\"]'@'content'");
        assertEquals("Fallback Title", result2);

        // Test 3: Both don't exist
        String result3 = crawler.executeAsString("D'h1.nonexistent'# ? D'h2.nonexistent'#");
        assertNull(result3);
    }

    @Test
    void testMapEach() {
        String html = """
            <html>
                <body>
                    <div class="post">
                        <h2>Post 1</h2>
                        <p>Content 1</p>
                    </div>
                    <div class="post">
                        <h2>Post 2</h2>
                        <p>Content 2</p>
                    </div>
                    <div class="post">
                        <h2>Post 3</h2>
                        <p>Content 3</p>
                    </div>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Extract all post titles using map each
        String titles = crawler.executeAsString("D'''div.post'''>'h2'#");
        assertNotNull(titles);
        assertTrue(titles.contains("Post 1"));
        assertTrue(titles.contains("Post 2"));
        assertTrue(titles.contains("Post 3"));
    }

    @Test
    void testComplexSelector() {
        String html = """
            <html>
                <body>
                    <article data-component="headline-block">
                        <h1>Article Headline</h1>
                    </article>
                    <article data-component="text-block">
                        <p>First paragraph of article</p>
                        <p>Second paragraph of article</p>
                    </article>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Test 1: Complex selector for headline
        String headline = crawler.executeAsString("D'article[data-component=\"headline-block\"] h1'#");
        assertEquals("Article Headline", headline);

        // Test 2: Multiple text blocks
        String content = crawler.executeAsString("D'''article[data-component=\"text-block\"] p'''>#");
        assertNotNull(content);
        assertTrue(content.contains("First paragraph"));
        assertTrue(content.contains("Second paragraph"));
    }

    @Test
    void testBBCNewsLikeDsl() {
        // BBC 뉴스와 유사한 구조
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="BBC News Fallback Title">
                    <meta property="og:image" content="https://example.com/bbc-image.jpg">
                </head>
                <body>
                    <article data-component="headline-block">
                        <h1>Breaking News: Test Article</h1>
                    </article>
                    <article data-component="text-block">
                        <p>This is the first paragraph of the news article.</p>
                        <p>This is the second paragraph with more details.</p>
                        <p>This is the third paragraph concluding the story.</p>
                    </article>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // BBC 스타일 DSL: 제목 추출 (h1이 있으면 h1, 없으면 og:title)
        String titleDsl = "D'article[data-component=\"headline-block\"] h1'# ? D'meta[property=\"og:title\"]'@'content'";
        String title = crawler.executeAsString(titleDsl);
        assertEquals("Breaking News: Test Article", title);

        // BBC 스타일 DSL: 본문 추출
        String contentDsl = "D'''article[data-component=\"text-block\"] p'''>#";
        String content = crawler.executeAsString(contentDsl);
        assertNotNull(content);
        assertTrue(content.contains("first paragraph"));
        assertTrue(content.contains("second paragraph"));
        assertTrue(content.contains("third paragraph"));

        // BBC 스타일 DSL: 이미지 추출
        String imageDsl = "D'meta[property=\"og:image\"]'@'content'";
        String image = crawler.executeAsString(imageDsl);
        assertEquals("https://example.com/bbc-image.jpg", image);
    }

    @Test
    void testActualBBCNews() throws IOException {
        // 실제 BBC 뉴스 크롤링 테스트
        String url = "https://www.bbc.com/news/articles/c04gvx7egw5o";

        System.out.println("Fetching BBC News: " + url);
        Document doc = Jsoup.connect(url)
            .timeout(10000)
            .userAgent("Mozilla/5.0")
            .get();

        CrawlerDsl crawler = new CrawlerDsl(doc);

        // BBC 제목 DSL
        String titleDsl = "D'article[data-component=\"headline-block\"] h1'# ? D'meta[property=\"og:title\"]'@'content'";
        String title = crawler.executeAsString(titleDsl);

        System.out.println("Title: " + title);
        assertNotNull(title, "Title should not be null");
        assertFalse(title.trim().isEmpty(), "Title should not be empty");

        // BBC 이미지 DSL
        String imageDsl = "D'meta[property=\"og:image\"]'@'content'";
        String image = crawler.executeAsString(imageDsl);

        System.out.println("Image URL: " + image);
        assertNotNull(image, "Image should not be null");
    }

    @Test
    void testEmptyAndNullCases() {
        String html = """
            <html>
                <body>
                    <div class="empty"></div>
                    <p></p>
                </body>
            </html>
            """;

        CrawlerDsl crawler = new CrawlerDsl(html);

        // Test 1: Non-existent element
        String result1 = crawler.executeAsString("D'h1.nonexistent'#");
        assertNull(result1);

        // Test 2: Empty element
        String result2 = crawler.executeAsString("D'div.empty'#");
        assertNull(result2); // Empty text should return null

        // Test 3: Empty DSL
        String result3 = crawler.executeAsString("");
        assertNull(result3);

        // Test 4: Null DSL
        String result4 = crawler.executeAsString(null);
        assertNull(result4);
    }
}
