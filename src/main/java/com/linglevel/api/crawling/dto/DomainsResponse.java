package com.linglevel.api.crawling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainsResponse {
    
    @Schema(description = "MongoDB ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "도메인명", example = "coupang.com")
    private String domain;

    @Schema(description = "사이트명", example = "쿠팡")
    private String name;

    @Schema(description = "Feed 콘텐츠 타입", example = "BLOG")
    private FeedContentType contentType;

    @Schema(description = "접근 조회용 URL")
    private String accessUrl;
}