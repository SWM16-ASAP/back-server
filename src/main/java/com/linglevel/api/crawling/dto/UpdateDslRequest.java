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
    
    @NotBlank(message = "Title DSL is required")
    @Schema(description = "업데이트할 제목 추출 DSL 규칙", example = "h1.new-product-title", required = true)
    private String titleDsl;
    
    @NotBlank(message = "Content DSL is required")
    @Schema(description = "업데이트할 본문 추출 DSL 규칙", example = ".new-product-description", required = true)
    private String contentDsl;
}