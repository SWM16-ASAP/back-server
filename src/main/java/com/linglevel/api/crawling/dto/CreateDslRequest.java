package com.linglevel.api.crawling.dto;

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
    
    @NotBlank(message = "Title DSL is required")
    @Schema(description = "제목 추출 DSL 규칙", example = "h1.product-title", required = true)
    private String titleDsl;
    
    @NotBlank(message = "Content DSL is required")
    @Schema(description = "본문 추출 DSL 규칙", example = ".product-description", required = true)
    private String contentDsl;
}