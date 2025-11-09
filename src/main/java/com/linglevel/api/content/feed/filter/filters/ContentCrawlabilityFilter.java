package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.content.feed.filter.FeedFilter;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.crawling.dsl.CrawlerDsl;
import com.linglevel.api.crawling.entity.CrawlingDsl;
import com.linglevel.api.crawling.repository.CrawlingDslRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.net.URL;

/**
 * 콘텐츠 크롤링 가능성 필터
 * CrawlingDsl을 사용하여 실제로 텍스트를 추출할 수 있는지 검증
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentCrawlabilityFilter implements FeedFilter {

    private static final String FILTER_NAME = "ContentCrawlabilityFilter";
    private final CrawlingDslRepository crawlingDslRepository;

    @Override
    public FeedFilterResult filter(SyndEntry entry, FeedSource feedSource) {
        String url = entry.getLink();

        if (url == null) {
            return FeedFilterResult.fail(FILTER_NAME, "URL is null");
        }

        String domain = extractDomain(url);
        if (domain == null) {
            log.warn("Failed to extract domain from URL: {}", url);
            return FeedFilterResult.pass(); // 도메인 추출 실패는 패스
        }

        if (!isCrawlable(url, domain)) {
            return FeedFilterResult.fail(FILTER_NAME,
                "Unable to extract content from URL: " + url);
        }

        return FeedFilterResult.pass();
    }

    private boolean isCrawlable(String url, String domain) {
        CrawlingDsl crawlingDsl = crawlingDslRepository.findByDomain(domain).orElse(null);

        if (crawlingDsl == null) {
            // DSL이 없으면 패스 (선택적 체크)
            log.debug("No CrawlingDsl found for domain: {}", domain);
            return true;
        }

        // 2. contentDsl이 없으면 패스
        if (crawlingDsl.getContentDsl() == null || crawlingDsl.getContentDsl().trim().isEmpty()) {
            log.debug("No contentDsl defined for domain: {}", domain);
            return true;
        }

        // 3. 실제 크롤링 시도
        try {
            log.debug("Testing crawlability for URL: {} with DSL", url);

            Document doc = Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0")
                .get();

            CrawlerDsl crawler = new CrawlerDsl(doc);
            String extractedContent = crawler.executeAsString(crawlingDsl.getContentDsl());

            // 추출된 콘텐츠가 없거나 너무 짧으면 실패
            if (extractedContent == null || extractedContent.trim().isEmpty()) {
                log.warn("No content extracted from URL: {}", url);
                return false;
            }

            // 콘텐츠가 너무 짧거나 길면 실패
            if (extractedContent.trim().length() < 100 || extractedContent.trim().length() > 15_000) {
                log.warn("Extracted content too short ({} chars) from URL: {}",
                    extractedContent.trim().length(), url);
                return false;
            }

            log.debug("Successfully extracted {} chars from URL: {}",
                extractedContent.trim().length(), url);
            return true;

        } catch (Exception e) {
            log.warn("Failed to test crawlability for URL: {}", url, e);
            // 크롤링 실패는 soft-delete 처리 (콘텐츠를 추출할 수 없음)
            return false;
        }
    }

    private String extractDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            log.warn("Failed to extract domain from URL: {}", urlString, e);
            return null;
        }
    }

    @Override
    public String getName() {
        return FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return 100; // 실제 HTTP 요청이 필요하므로 가장 나중에 실행
    }
}
