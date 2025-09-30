package com.linglevel.api.content.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentType {
    BOOK("BOOK", "책과 소설 등의 긴 형태의 콘텐츠"),
    ARTICLE("ARTICLE", "뉴스, 블로그 포스트 등의 짧은 형태의 콘텐츠"),
    CUSTOM("CUSTOM", "사용자가 직접 가져온 콘텐츠");

    private final String code;
    private final String description;

    public boolean isBook() {
        return this == BOOK;
    }

    public boolean isArticle() {
        return this == ARTICLE;
    }
}
