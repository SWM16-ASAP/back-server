package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.content.feed.filter.FeedFilter;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 언어 필터
 * 영어가 아닌 언어로 작성된 타이틀을 필터링
 */
@Component
@Slf4j
public class LanguageFilter implements FeedFilter {

    private static final String FILTER_NAME = "LanguageFilter";

    @Override
    public FeedFilterResult filter(SyndEntry entry, FeedSource feedSource) {
        String title = entry.getTitle();

        if (title == null || title.trim().isEmpty()) {
            return FeedFilterResult.fail(FILTER_NAME, "Empty title");
        }

        if (isNonEnglishTitle(title)) {
            return FeedFilterResult.fail(FILTER_NAME,
                "Non-English title detected: " + title.substring(0, Math.min(50, title.length())));
        }

        return FeedFilterResult.pass();
    }

    private boolean isNonEnglishTitle(String title) {
        // 비영어권 문자 감지: 한글, 일본어, 중국어, 힌디어, 아랍어 등
        for (char c : title.toCharArray()) {
            // 한글: 0xAC00-0xD7AF (완성형), 0x1100-0x11FF (자모)
            if ((c >= 0xAC00 && c <= 0xD7AF) || (c >= 0x1100 && c <= 0x11FF)) {
                log.debug("Korean character detected: {}", c);
                return true;
            }

            // 일본어: 히라가나(0x3040-0x309F), 카타카나(0x30A0-0x30FF)
            if ((c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF)) {
                log.debug("Japanese character detected: {}", c);
                return true;
            }

            // 중국어/한자: CJK Unified Ideographs (0x4E00-0x9FFF)
            if (c >= 0x4E00 && c <= 0x9FFF) {
                log.debug("Chinese/CJK character detected: {}", c);
                return true;
            }

            // 힌디어/데바나가리: 0x0900-0x097F
            if (c >= 0x0900 && c <= 0x097F) {
                log.debug("Hindi/Devanagari character detected: {}", c);
                return true;
            }

            // 아랍어: 0x0600-0x06FF
            if (c >= 0x0600 && c <= 0x06FF) {
                log.debug("Arabic character detected: {}", c);
                return true;
            }

            // 태국어: 0x0E00-0x0E7F
            if (c >= 0x0E00 && c <= 0x0E7F) {
                log.debug("Thai character detected: {}", c);
                return true;
            }

            // 키릴 문자 (러시아어 등): 0x0400-0x04FF
            if (c >= 0x0400 && c <= 0x04FF) {
                log.debug("Cyrillic character detected: {}", c);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return 20; // URL 체크 다음으로 빠름
    }
}
