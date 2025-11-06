package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "FeedSource 응답")
public class FeedSourceResponse {

    @Schema(description = "FeedSource ID")
    private String id;

    @Schema(description = "RSS Feed URL")
    private String url;

    @Schema(description = "도메인")
    private String domain;

    @Schema(description = "FeedSource 이름")
    private String name;

    @Schema(description = "커버 이미지 추출 DSL")
    private String coverImageDsl;

    @Schema(description = "콘텐츠 타입")
    private FeedContentType contentType;

    @Schema(description = "카테고리")
    private ContentCategory category;

    @Schema(description = "태그 목록")
    private List<String> tags;

    @Schema(description = "활성화 여부")
    private Boolean isActive;

    @Schema(description = "생성일")
    private Instant createdAt;

    @Schema(description = "수정일")
    private Instant updatedAt;
}
