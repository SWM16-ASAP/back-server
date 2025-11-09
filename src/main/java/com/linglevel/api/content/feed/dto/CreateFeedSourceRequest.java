package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FeedSource 생성 요청")
public class CreateFeedSourceRequest {

    @NotBlank(message = "URL is required")
    @Schema(description = "RSS Feed URL", example = "https://www.bbc.com/news/technology/rss.xml")
    private String url;

    @NotBlank(message = "Name is required")
    @Schema(description = "FeedSource 이름", example = "BBC Technology News")
    private String name;

    @Schema(description = "커버 이미지 추출 DSL (선택, RSS에 썸네일이 없을 경우)", example = "doc > meta[property=og:image] @ content")
    private String coverImageDsl;

    @NotNull(message = "Content type is required")
    @Schema(description = "콘텐츠 타입", example = "NEWS")
    private FeedContentType contentType;

    @NotNull(message = "Category is required")
    @Schema(description = "카테고리", example = "TECH")
    private ContentCategory category;

    @Schema(description = "태그 목록", example = "[\"Technology\", \"AI\", \"News\"]")
    private List<String> tags;
}
