package com.linglevel.api.crawling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDslRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "업데이트할 사이트명", example = "쿠팡", required = true)
    private String name;

    @NotBlank(message = "Title DSL is required")
    @Schema(description = "업데이트할 제목 추출 DSL 규칙", example = "h1.new-product-title", required = true)
    private String titleDsl;
    
    @NotBlank(message = "Content DSL is required")
    @Schema(description = "업데이트할 본문 추출 DSL 규칙", example = ".new-product-description", required = true)
    private String contentDsl;

    @Schema(description = "업데이트할 섬네일 추출 DSL 규칙", example = "meta[property='og:image']", required = false)
    private String thumbnailDsl;
}