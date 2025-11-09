package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.content.feed.filter.FeedFilter;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * YouTube Shorts 필터
 * YouTube Shorts 영상을 필터링
 */
@Component
@Slf4j
public class YouTubeShortsFilter implements FeedFilter {

    private static final String FILTER_NAME = "YouTubeShortsFilter";

    @Override
    public FeedFilterResult filter(SyndEntry entry, FeedSource feedSource) {
        String url = entry.getLink();

        if (url == null) {
            return FeedFilterResult.pass();
        }

        // TODO: YouTube Shorts 판별 로직 구현
        // 1. URL 패턴 체크: youtube.com/shorts/xxx
        // 2. Video Duration 체크 (60초 이하)
        // 3. Media RSS의 duration 속성 체크

        if (isYouTubeShorts(url, entry)) {
            return FeedFilterResult.fail(FILTER_NAME, "YouTube Shorts video detected");
        }

        return FeedFilterResult.pass();
    }

    private boolean isYouTubeShorts(String url, SyndEntry entry) {
        // TODO: 실제 구현
        // - URL 패턴 매칭
        // - Duration 체크 (media:content의 duration 속성)
        return false;
    }

    @Override
    public String getName() {
        return FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return 10; // 빠른 URL 체크이므로 우선순위 높음
    }
}
