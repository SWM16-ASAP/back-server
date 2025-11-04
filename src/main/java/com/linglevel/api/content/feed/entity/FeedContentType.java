package com.linglevel.api.content.feed.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedContentType {
    YOUTUBE("YOUTUBE", "유튜브", "유튜브 비디오 콘텐츠"),
    BLOG("BLOG", "블로그", "블로그 포스트"),
    NEWS("NEWS", "뉴스", "뉴스 기사");

    private final String code;
    private final String displayName;
    private final String description;
}
