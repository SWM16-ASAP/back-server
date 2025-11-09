package com.linglevel.api.content.feed.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 피드 필터링 결과
 */
@Getter
@Builder
@AllArgsConstructor
public class FeedFilterResult {

    private final boolean passed;

    private final String reason;

    private final String filterName;

    public static FeedFilterResult pass() {
        return FeedFilterResult.builder()
            .passed(true)
            .build();
    }

    public static FeedFilterResult fail(String filterName, String reason) {
        return FeedFilterResult.builder()
            .passed(false)
            .filterName(filterName)
            .reason(reason)
            .build();
    }
}
