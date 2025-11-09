package com.linglevel.api.content.feed.service;

import com.linglevel.api.crawling.dsl.CrawlerDsl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("고급 커버 이미지 DSL 테스트")
class AdvancedCoverImageDslTest {

    @Test
    @DisplayName("정교한 fallback 체인 DSL로 커버 이미지 추출")
    void extractCoverImageWithAdvancedDsl() throws Exception {
        // given: 고급 커버 이미지 DSL (여러 fallback)
        String coverImageDsl = "D'meta[property=\"og:image\"]'@'content' ? " +
            "D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article .hero-image img, article .main-image img, article .featured-image img'@'src'";

        // given: 다양한 article URLs (Medium, BBC, TechCrunch 등)
        String[] testUrls = {
            "https://python.plainenglish.io/the-ai-workflow-that-runs-my-business-while-i-sleep-def4424d29cd",
            "https://medium.com/codetodeploy/how-i-used-machine-learning-to-predict-my-business-metrics-89cca3c59ec6",
            "https://techcrunch.com/2024/11/05/anthropics-new-ai-can-control-your-pc/"
        };

        System.out.println("===========================================");
        System.out.println("=== 고급 Fallback 체인 DSL 테스트 ===");
        System.out.println("===========================================");
        System.out.println("DSL:");
        System.out.println("1. og:image 메타 태그");
        System.out.println("2. article figure img (로고/광고 제외)");
        System.out.println("3. article picture img (로고/광고 제외)");
        System.out.println("4. article hero/main/featured 이미지");
        System.out.println("===========================================\n");

        for (String url : testUrls) {
            try {
                System.out.println("--- " + url + " ---");

                Document doc = Jsoup.connect(url)
                    .timeout(15000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

                CrawlerDsl crawler = new CrawlerDsl(doc);
                String coverImage = crawler.executeAsString(coverImageDsl);

                if (coverImage != null && !coverImage.isEmpty()) {
                    System.out.println("✓ 커버 이미지 추출 성공");
                    System.out.println("  URL: " + coverImage);

                    // 어느 fallback에서 추출되었는지 확인
                    String ogImage = crawler.executeAsString("D'meta[property=\"og:image\"]'@'content'");
                    if (ogImage != null && ogImage.equals(coverImage)) {
                        System.out.println("  Method: og:image (1st fallback)");
                    } else {
                        System.out.println("  Method: 다른 fallback 사용됨");
                    }
                } else {
                    System.out.println("✗ 커버 이미지 추출 실패");
                }

                System.out.println();

            } catch (Exception e) {
                System.out.println("✗ 에러: " + e.getMessage());
                System.out.println();
            }
        }

        System.out.println("===========================================");
    }

    @Test
    @DisplayName("각 fallback 단계별 테스트")
    void testEachFallbackStep() throws Exception {
        // given: Medium article
        String url = "https://python.plainenglish.io/the-ai-workflow-that-runs-my-business-while-i-sleep-def4424d29cd";

        System.out.println("===========================================");
        System.out.println("=== Fallback 단계별 분석 ===");
        System.out.println("===========================================");
        System.out.println("URL: " + url);
        System.out.println();

        Document doc = Jsoup.connect(url)
            .timeout(15000)
            .userAgent("Mozilla/5.0")
            .get();

        CrawlerDsl crawler = new CrawlerDsl(doc);

        // Fallback 1: og:image
        System.out.println("1. og:image 메타 태그:");
        String ogImage = crawler.executeAsString("D'meta[property=\"og:image\"]'@'content'");
        System.out.println("   " + (ogImage != null ? "✓ " + ogImage : "✗ 없음"));
        System.out.println();

        // Fallback 2: article figure img
        System.out.println("2. article figure img (필터링):");
        String figureImg = crawler.executeAsString(
            "D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src'"
        );
        System.out.println("   " + (figureImg != null ? "✓ " + figureImg : "✗ 없음"));
        System.out.println();

        // Fallback 3: article picture img
        System.out.println("3. article picture img (필터링):");
        String pictureImg = crawler.executeAsString(
            "D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src'"
        );
        System.out.println("   " + (pictureImg != null ? "✓ " + pictureImg : "✗ 없음"));
        System.out.println();

        // Fallback 4: hero/main/featured images
        System.out.println("4. hero/main/featured 이미지:");
        String heroImg = crawler.executeAsString("D'article .hero-image img, article .main-image img, article .featured-image img'@'src'");
        System.out.println("   " + (heroImg != null ? "✓ " + heroImg : "✗ 없음"));
        System.out.println();

        // 최종 결과 (전체 체인)
        System.out.println("===========================================");
        System.out.println("최종 결과 (전체 fallback 체인):");
        String finalResult = crawler.executeAsString(
            "D'meta[property=\"og:image\"]'@'content' ? " +
            "D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article .hero-image img, article .main-image img, article .featured-image img'@'src'"
        );
        System.out.println(finalResult != null ? "✓ " + finalResult : "✗ 실패");
        System.out.println("===========================================");

        assertNotNull(finalResult, "커버 이미지를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("다양한 뉴스 사이트에서 커버 이미지 추출")
    void extractFromVariousNewsSites() throws Exception {
        String coverImageDsl = "D'meta[property=\"og:image\"]'@'content' ? " +
            "D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article .hero-image img, article .main-image img, article .featured-image img'@'src'";

        String[][] testSites = {
            {"Medium", "https://python.plainenglish.io/the-ai-workflow-that-runs-my-business-while-i-sleep-def4424d29cd"},
            {"TechCrunch", "https://techcrunch.com/2024/11/05/anthropics-new-ai-can-control-your-pc/"},
            {"The Verge", "https://www.theverge.com/2024/11/5/24288788/anthropic-ai-model-computer-use-control-pc"}
        };

        System.out.println("===========================================");
        System.out.println("=== 다양한 뉴스 사이트 테스트 ===");
        System.out.println("===========================================\n");

        int successCount = 0;
        int totalCount = testSites.length;

        for (String[] site : testSites) {
            String siteName = site[0];
            String url = site[1];

            try {
                System.out.println("--- " + siteName + " ---");
                System.out.println("URL: " + url);

                Document doc = Jsoup.connect(url)
                    .timeout(15000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

                CrawlerDsl crawler = new CrawlerDsl(doc);
                String coverImage = crawler.executeAsString(coverImageDsl);

                if (coverImage != null && !coverImage.isEmpty()) {
                    System.out.println("✓ 성공");
                    System.out.println("Image: " + (coverImage.length() > 80 ? coverImage.substring(0, 80) + "..." : coverImage));
                    successCount++;
                } else {
                    System.out.println("✗ 실패");
                }

            } catch (Exception e) {
                System.out.println("✗ 에러: " + e.getMessage());
            }

            System.out.println();
        }

        System.out.println("===========================================");
        System.out.println("결과: " + successCount + "/" + totalCount + " 성공");
        System.out.println("성공률: " + String.format("%.1f%%", (successCount * 100.0 / totalCount)));
        System.out.println("===========================================");
    }

    @Test
    @DisplayName("권장 DSL 출력")
    void printRecommendedDsl() {
        System.out.println("===========================================");
        System.out.println("=== FeedSource에 사용할 권장 DSL ===");
        System.out.println("===========================================\n");

        String recommendedDsl = "D'meta[property=\"og:image\"]'@'content' ? " +
            "D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? " +
            "D'article .hero-image img, article .main-image img, article .featured-image img'@'src'";

        System.out.println("coverImageDsl:");
        System.out.println(recommendedDsl);
        System.out.println();

        System.out.println("설명:");
        System.out.println("1. og:image 메타 태그 (가장 안정적)");
        System.out.println("2. article figure img (로고, 광고, 아바타 제외)");
        System.out.println("3. article picture img (로고, 광고, 아바타 제외)");
        System.out.println("4. article의 hero/main/featured 이미지");
        System.out.println();

        System.out.println("API 요청 예시:");
        System.out.println("{");
        System.out.println("  \"url\": \"https://medium.com/feed/tag/programming\",");
        System.out.println("  \"name\": \"Medium Programming\",");
        System.out.println("  \"coverImageDsl\": \"" + recommendedDsl.replace("\"", "\\\"") + "\",");
        System.out.println("  \"contentType\": \"NEWS\",");
        System.out.println("  \"category\": \"TECH\"");
        System.out.println("}");

        System.out.println("\n===========================================");
    }
}
