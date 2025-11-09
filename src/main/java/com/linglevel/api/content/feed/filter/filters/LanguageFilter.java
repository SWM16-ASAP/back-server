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

        // TODO: 언어 감지 로직 구현
        // 1. 한글/일본어/중국어 등 비영어권 문자 감지
        // 2. 또는 language detection library 사용 (e.g., Apache Tika)

        if (isNonEnglishTitle(title)) {
            return FeedFilterResult.fail(FILTER_NAME,
                "Non-English title detected: " + title.substring(0, Math.min(50, title.length())));
        }

        return FeedFilterResult.pass();
    }

    private boolean isNonEnglishTitle(String title) {
        // TODO: 실제 구현
        // 방법 1: 유니코드 범위로 감지
        // - 한글: 0xAC00-0xD7AF
        // - 일본어 히라가나/카타카나: 0x3040-0x30FF
        // - 중국어: 0x4E00-0x9FFF

        // 방법 2: Language detection library
        // - Apache Tika LanguageIdentifier
        // - com.optimaize.langdetect

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
