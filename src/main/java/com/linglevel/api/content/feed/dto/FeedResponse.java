package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "피드 응답")
public class FeedResponse {

    @Schema(description = "피드 ID", example = "60d0fe4f5311236168a109ca")
    private String id;

    @Schema(description = "콘텐츠 타입", example = "YOUTUBE")
    private FeedContentType contentType;

    @Schema(description = "제목", example = "AI의 미래: 2024년 트렌드")
    private String title;

    @Schema(description = "URL", example = "https://example.com/article/ai-future")
    private String url;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/images/thumbnail.jpg")
    private String thumbnailUrl;

    @Schema(description = "작성자", example = "John Doe")
    private String author;

    @Schema(description = "설명", example = "AI 기술의 최신 동향과 2024년 전망에 대해 알아봅니다.")
    private String description;

    @Schema(description = "카테고리", example = "TECH")
    private ContentCategory category;

    @Schema(description = "태그 목록", example = "[\"AI\", \"Technology\", \"Trends\"]")
    private List<String> tags;

    @Schema(description = "소스 제공자", example = "bbc.com")
    private String sourceProvider;

    @Schema(description = "발행일", example = "2024-01-15T09:30:00Z")
    private Instant publishedAt;

    @Schema(description = "조회수", example = "1520")
    private Integer viewCount;

    @Schema(description = "평균 읽기 시간 (초)", example = "180.5")
    private Double avgReadTimeSeconds;

    @Schema(description = "생성일", example = "2024-01-15T10:00:00Z")
    private Instant createdAt;
}
