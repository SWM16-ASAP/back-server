package com.linglevel.api.content.common.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 콘텐츠 기본 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentInfo {

    private String title;
    private String author;
    private String coverImageUrl;
    private Integer readingTime;

    public boolean isPresent() {
        return title != null;
    }
}