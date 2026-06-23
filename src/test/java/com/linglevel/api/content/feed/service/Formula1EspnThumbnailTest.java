package com.linglevel.api.content.feed.service;

import com.linglevel.api.crawling.dsl.CrawlerDsl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Formula1 & ESPN 썸네일 추출 DSL 테스트")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true",
		disabledReason = "외부 RSS/웹 페이지 의존 통합 테스트는 CI 환경에서 불안정하여 로컬에서만 실행")
class Formula1EspnThumbnailTest {

	// 권장 DSL (다양한 사이트에서 작동)
	private static final String RECOMMENDED_DSL = "D'meta[property=\"og:image\"]'@'content' ? D'article figure img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? D'article picture img:not([class=\"logo\"]):not([id=\"logo\"]):not([class=\"brand\"]):not([id=\"brand\"]):not([class=\"icon\"]):not([src=\".svg\"]):not([class=\"ad\"]):not([id=\"ad\"]):not([class=\"advert\"]):not([id=\"advert\"]):not([class=\"avatar\"]):not([class=\"profile\"])'@'src' ? D'article .hero-image img, article .main-image img, article .featured-image img'@'src'";

	@Test
	@DisplayName("Formula1 article에서 썸네일 추출 DSL 테스트")
	void testFormula1ThumbnailExtraction() throws Exception {
		// given: Formula1 RSS에서 article URL 가져오기
		String rssUrl = "https://www.formula1.com/en/latest/all.xml";

		SyndFeedInput input = new SyndFeedInput();
		input.setAllowDoctypes(true);
		SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
		List<SyndEntry> entries = feed.getEntries();

		System.out.println("\n=== Formula1 썸네일 추출 테스트 ===\n");
		System.out.println("RSS Feed: " + feed.getTitle());
		System.out.println("Total Entries: " + entries.size());
		System.out.println();

		int successCount = 0;
		int testLimit = Math.min(3, entries.size());

		for (int i = 0; i < testLimit; i++) {
			SyndEntry entry = entries.get(i);
			String articleUrl = entry.getLink();

			System.out.println("--- Article " + (i + 1) + " ---");
			System.out.println("Title: " + entry.getTitle());
			System.out.println("URL: " + articleUrl);

			try {
				// Article 페이지 크롤링
				Document doc = Jsoup.connect(articleUrl)
					.timeout(15000)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
					.get();

				CrawlerDsl crawler = new CrawlerDsl(doc);

				// 권장 DSL로 썸네일 추출
				String thumbnail = crawler.executeAsString(RECOMMENDED_DSL);

				if (thumbnail != null && !thumbnail.trim().isEmpty()) {
					System.out.println("✓ 썸네일 추출 성공");
					System.out.println(
							"Thumbnail: " + (thumbnail.length() > 80 ? thumbnail.substring(0, 80) + "..." : thumbnail));
					successCount++;
				}
				else {
					System.out.println("✗ 썸네일 추출 실패");

					// 실패한 경우 og:image만 시도
					String ogImage = crawler.executeAsString("D'meta[property=\"og:image\"]'@'content'");
					if (ogImage != null) {
						System.out.println("  og:image: " + ogImage);
					}
				}

			}
			catch (Exception e) {
				System.out.println("✗ 크롤링 에러: " + e.getMessage());
			}

			System.out.println();
		}

		System.out.println("결과: " + successCount + "/" + testLimit + " 성공");
		System.out.println("성공률: " + String.format("%.1f%%", (successCount * 100.0 / testLimit)));
		System.out.println();

		assertTrue(successCount > 0, "최소 1개 이상의 썸네일이 추출되어야 함");
	}

	@Test
	@DisplayName("ESPN MLB article에서 썸네일 추출 DSL 테스트")
	void testEspnMlbThumbnailExtraction() throws Exception {
		// given: ESPN MLB RSS에서 article URL 가져오기
		String rssUrl = "https://www.espn.com/espn/rss/mlb/news";

		SyndFeedInput input = new SyndFeedInput();
		input.setAllowDoctypes(true);
		SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
		List<SyndEntry> entries = feed.getEntries();

		System.out.println("\n=== ESPN MLB 썸네일 추출 테스트 ===\n");
		System.out.println("RSS Feed: " + feed.getTitle());
		System.out.println("Total Entries: " + entries.size());
		System.out.println();

		int successCount = 0;
		int testLimit = Math.min(3, entries.size());

		for (int i = 0; i < testLimit; i++) {
			SyndEntry entry = entries.get(i);
			String articleUrl = entry.getLink();

			System.out.println("--- Article " + (i + 1) + " ---");
			System.out.println("Title: " + entry.getTitle());
			System.out.println("URL: " + articleUrl);

			try {
				// Article 페이지 크롤링
				Document doc = Jsoup.connect(articleUrl)
					.timeout(15000)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
					.get();

				CrawlerDsl crawler = new CrawlerDsl(doc);

				// 권장 DSL로 썸네일 추출
				String thumbnail = crawler.executeAsString(RECOMMENDED_DSL);

				if (thumbnail != null && !thumbnail.trim().isEmpty()) {
					System.out.println("✓ 썸네일 추출 성공");
					System.out.println(
							"Thumbnail: " + (thumbnail.length() > 80 ? thumbnail.substring(0, 80) + "..." : thumbnail));
					successCount++;
				}
				else {
					System.out.println("✗ 썸네일 추출 실패");

					// 실패한 경우 og:image만 시도
					String ogImage = crawler.executeAsString("D'meta[property=\"og:image\"]'@'content'");
					if (ogImage != null) {
						System.out.println("  og:image: " + ogImage);
					}
				}

			}
			catch (Exception e) {
				System.out.println("✗ 크롤링 에러: " + e.getMessage());
			}

			System.out.println();
		}

		System.out.println("결과: " + successCount + "/" + testLimit + " 성공");
		System.out.println("성공률: " + String.format("%.1f%%", (successCount * 100.0 / testLimit)));
		System.out.println();

		assertTrue(successCount > 0, "최소 1개 이상의 썸네일이 추출되어야 함");
	}

	@Test
	@DisplayName("Formula1 & ESPN 통합 썸네일 DSL 분석")
	void analyzeThumbnailDslForBothSites() throws Exception {
		String[][] sources = { { "Formula1", "https://www.formula1.com/en/latest/all.xml" },
				{ "ESPN MLB", "https://www.espn.com/espn/rss/mlb/news" } };

		System.out.println("\n=== Formula1 & ESPN 통합 썸네일 DSL 분석 ===\n");

		for (String[] source : sources) {
			String sourceName = source[0];
			String rssUrl = source[1];

			try {
				System.out.println("📰 " + sourceName);
				System.out.println("─────────────────────────────────────");

				SyndFeedInput input = new SyndFeedInput();
				input.setAllowDoctypes(true);
				SyndFeed feed = input.build(new XmlReader(new URL(rssUrl)));
				List<SyndEntry> entries = feed.getEntries();

				if (entries.isEmpty()) {
					System.out.println("⚠️ 엔트리가 없습니다.");
					System.out.println();
					continue;
				}

				// 첫 번째 article만 상세 분석
				SyndEntry firstEntry = entries.get(0);
				String articleUrl = firstEntry.getLink();

				System.out.println("Sample Article: " + firstEntry.getTitle());
				System.out.println("URL: " + articleUrl);
				System.out.println();

				Document doc = Jsoup.connect(articleUrl)
					.timeout(15000)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
					.get();

				CrawlerDsl crawler = new CrawlerDsl(doc);

				// 각 fallback 단계별 테스트
				System.out.println("1. og:image 메타 태그:");
				String ogImage = crawler.executeAsString("D'meta[property=\"og:image\"]'@'content'");
				System.out.println("   " + (ogImage != null ? "✓ " + ogImage : "✗ 없음"));

				System.out.println("2. article figure img:");
				String figureImg = crawler.executeAsString("D'article figure img'@'src'");
				System.out.println("   " + (figureImg != null ? "✓ " + figureImg : "✗ 없음"));

				System.out.println("3. article picture img:");
				String pictureImg = crawler.executeAsString("D'article picture img'@'src'");
				System.out.println("   " + (pictureImg != null ? "✓ " + pictureImg : "✗ 없음"));

				System.out.println("4. hero/main/featured 이미지:");
				String heroImg = crawler.executeAsString(
						"D'article .hero-image img, article .main-image img, article .featured-image img'@'src'");
				System.out.println("   " + (heroImg != null ? "✓ " + heroImg : "✗ 없음"));

				System.out.println();
				System.out.println("최종 결과 (권장 DSL):");
				String finalResult = crawler.executeAsString(RECOMMENDED_DSL);
				System.out.println(finalResult != null ? "✓ " + finalResult : "✗ 실패");

				System.out.println();

			}
			catch (Exception e) {
				System.out.println("✗ 에러: " + e.getMessage());
				System.out.println();
			}
		}

		System.out.println("===========================================");
	}

	@Test
	@DisplayName("권장 DSL 출력 (Formula1 & ESPN용)")
	void printRecommendedDslForNewSources() {
		System.out.println("\n===========================================");
		System.out.println("=== Formula1 & ESPN FeedSource 설정 권장사항 ===");
		System.out.println("===========================================\n");

		System.out.println("coverImageDsl (권장):");
		System.out.println(RECOMMENDED_DSL);
		System.out.println();

		System.out.println("설명:");
		System.out.println("- 이 DSL은 Formula1, ESPN 등 대부분의 뉴스 사이트에서 작동합니다");
		System.out.println("- RSS에 썸네일이 없어도 article 페이지에서 자동으로 추출합니다");
		System.out.println("- 4단계 fallback으로 높은 성공률을 보장합니다");
		System.out.println();

		System.out.println("간소화된 DSL (og:image만 사용):");
		String simpleDsl = "D'meta[property=\"og:image\"]'@'content'";
		System.out.println(simpleDsl);
		System.out.println("- 대부분의 사이트에서 og:image는 잘 설정되어 있습니다");
		System.out.println("- 더 빠른 크롤링이 필요한 경우 이 옵션을 사용하세요");
		System.out.println();

		System.out.println("===========================================");
	}

}
