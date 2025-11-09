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

        // YouTube URL이 아니면 패스
        if (!url.contains("youtube.com")) {
            return FeedFilterResult.pass();
        }

        if (isYouTubeShorts(url, entry)) {
            return FeedFilterResult.fail(FILTER_NAME, "YouTube Shorts video detected");
        }

        return FeedFilterResult.pass();
    }

    private boolean isYouTubeShorts(String url, SyndEntry entry) {
        // YouTube Shorts는 /shorts/ 경로 사용
        if (url.contains("/shorts/")) {
            log.debug("YouTube Shorts detected by URL pattern: {}", url);
            return true;
        }

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
