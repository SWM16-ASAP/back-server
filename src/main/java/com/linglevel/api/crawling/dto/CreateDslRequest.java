package com.linglevel.api.crawling.dto;

import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDslRequest {
    
    @NotBlank(message = "Domain is required")
    @Schema(description = "도메인명", example = "coupang.com", required = true)
    private String domain;

    @NotBlank(message = "Name is required")
    @Schema(description = "사이트명", example = "쿠팡", required = true)
    private String name;

    @Schema(description = "Feed 콘텐츠 타입", example = "BLOG", required = false)
    private FeedContentType contentType;

    @NotBlank(message = "Title DSL is required")
    @Schema(description = "제목 추출 DSL 규칙", example = "h1.product-title", required = true)
    private String titleDsl;
    
    @NotBlank(message = "Content DSL is required")
    @Schema(description = "본문 추출 DSL 규칙", example = ".product-description", required = true)
    private String contentDsl;

    @Schema(description = "커버 이미지 추출 DSL 규칙", example = "meta[property='og:image']", required = false)
    private String coverImageDsl;

    @Schema(description = "접근 조회용 URL", example = "https://www.coupang.com", required = false)
    private String accessUrl;
}