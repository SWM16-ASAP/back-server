package com.linglevel.api.content.feed.service;

import com.linglevel.api.crawling.dsl.CrawlerDsl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Medium 커버 이미지 추출 테스트")
class MediumCoverImageTest {

    @Test
    @DisplayName("Medium article에서 제공된 DSL로 커버 이미지 추출")
    void extractCoverImageFromMediumArticle() throws Exception {
        // given: Medium article URL (테스트에서 가져온 실제 URL)
        String articleUrl = "https://python.plainenglish.io/the-ai-workflow-that-runs-my-business-while-i-sleep-def4424d29cd";

        // given: 제공된 커버 이미지 DSL
        String coverImageDsl = "D'''article [data-component=\"article-body\"], article [data-testid=\"article-body\"], article [itemprop=\"articleBody\"], article .article-body__content, article .article-body, article .body__content''' > '''p:not([class*=\"caption\"]):not([class*=\"credit\"]):not([class*=\"byline\"]):not([class*=\"author\"]):not([class*=\"date\"]):not([class*=\"timestamp\"]):not([class*=\"dateline\"]):not([class*=\"note\"])''' > #^";

        System.out.println("===========================================");
        System.out.println("=== Medium Article 분석 ===");
        System.out.println("===========================================");
        System.out.println("URL: " + articleUrl);
        System.out.println();

        // when: Article HTML 가져오기
        Document doc = Jsoup.connect(articleUrl)
            .timeout(15000)
            .userAgent("Mozilla/5.0")
            .get();

        System.out.println("HTML 페이지 로드 완료");
        System.out.println();

        // DSL로 커버 이미지 추출 시도
        CrawlerDsl crawler = new CrawlerDsl(doc);
        String result = crawler.executeAsString(coverImageDsl);

        System.out.println("=== DSL 실행 결과 ===");
        if (result != null && !result.trim().isEmpty()) {
            System.out.println("추출된 내용 (첫 500자):");
            System.out.println(result.substring(0, Math.min(500, result.length())));
            if (result.length() > 500) {
                System.out.println("... (총 " + result.length() + "자)");
            }
        } else {
            System.out.println("추출 실패 또는 비어있음");
        }
        System.out.println();

        // 다른 방법들도 시도
        System.out.println("=== 대체 방법 시도 ===");

        // 1. og:image 메타 태그
        String ogImageDsl = "D'meta[property=\"og:image\"]'@'content'";
        String ogImage = crawler.executeAsString(ogImageDsl);
        System.out.println("og:image: " + ogImage);

        // 2. twitter:image 메타 태그
        String twitterImageDsl = "D'meta[name=\"twitter:image\"]'@'content'";
        String twitterImage = crawler.executeAsString(twitterImageDsl);
        System.out.println("twitter:image: " + twitterImage);

        // 3. article 내 첫 번째 img
        String firstImgDsl = "D'article img'@'src'";
        String firstImg = crawler.executeAsString(firstImgDsl);
        System.out.println("첫 번째 img src: " + firstImg);

        // 4. picture 태그
        String pictureDsl = "D'article picture img'@'src'";
        String pictureImg = crawler.executeAsString(pictureDsl);
        System.out.println("picture img: " + pictureImg);

        System.out.println();
        System.out.println("===========================================");

        // 가장 좋은 이미지 선택
        String bestImage = null;
        if (ogImage != null && !ogImage.isEmpty()) {
            bestImage = ogImage;
            System.out.println("선택된 이미지: og:image");
        } else if (twitterImage != null && !twitterImage.isEmpty()) {
            bestImage = twitterImage;
            System.out.println("선택된 이미지: twitter:image");
        } else if (firstImg != null && !firstImg.isEmpty()) {
            bestImage = firstImg;
            System.out.println("선택된 이미지: 첫 번째 img");
        } else if (pictureImg != null && !pictureImg.isEmpty()) {
            bestImage = pictureImg;
            System.out.println("선택된 이미지: picture img");
        }

        System.out.println("최종 커버 이미지 URL: " + bestImage);
        System.out.println("===========================================");

        assertNotNull(bestImage, "커버 이미지를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("Medium 여러 article에서 커버 이미지 추출 테스트")
    void extractCoverImagesFromMultipleArticles() throws Exception {
        // given: 여러 Medium article URLs
        String[] articleUrls = {
            "https://python.plainenglish.io/the-ai-workflow-that-runs-my-business-while-i-sleep-def4424d29cd",
            "https://medium.com/codetodeploy/how-i-used-machine-learning-to-predict-my-business-metrics-89cca3c59ec6",
            "https://ai.gopubby.com/the-ai-automation-that-cut-my-operating-costs-by-40-5c8d715e9f17"
        };

        System.out.println("===========================================");
        System.out.println("=== 여러 Medium Article 커버 이미지 추출 ===");
        System.out.println("===========================================\n");

        for (String url : articleUrls) {
            try {
                System.out.println("--- " + url + " ---");

                Document doc = Jsoup.connect(url)
                    .timeout(15000)
                    .userAgent("Mozilla/5.0")
                    .get();

                CrawlerDsl crawler = new CrawlerDsl(doc);

                // og:image 시도
                String ogImageDsl = "D'meta[property=\"og:image\"]'@'content'";
                String ogImage = crawler.executeAsString(ogImageDsl);

                if (ogImage != null && !ogImage.isEmpty()) {
                    System.out.println("✓ 커버 이미지 발견: " + ogImage);
                } else {
                    System.out.println("✗ 커버 이미지 없음");
                }

                System.out.println();

            } catch (Exception e) {
                System.out.println("✗ 실패: " + e.getMessage());
                System.out.println();
            }
        }

        System.out.println("===========================================");
    }

    @Test
    @DisplayName("Medium에서 권장되는 커버 이미지 DSL")
    void recommendedCoverImageDsl() {
        System.out.println("===========================================");
        System.out.println("=== Medium 커버 이미지 추출 권장 DSL ===");
        System.out.println("===========================================\n");

        System.out.println("1. 가장 안정적 (og:image 메타 태그):");
        System.out.println("   D'meta[property=\"og:image\"]'@'content'");
        System.out.println();

        System.out.println("2. Fallback 1 (twitter:image):");
        System.out.println("   D'meta[name=\"twitter:image\"]'@'content'");
        System.out.println();

        System.out.println("3. Fallback 2 (article 첫 이미지):");
        System.out.println("   D'article img'@'src'");
        System.out.println();

        System.out.println("4. 조합 DSL (null coalescing):");
        System.out.println("   D'meta[property=\"og:image\"]'@'content' ? D'meta[name=\"twitter:image\"]'@'content' ? D'article img'@'src'");
        System.out.println();

        System.out.println("===========================================");
    }
}
