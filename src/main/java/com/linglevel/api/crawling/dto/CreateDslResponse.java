package com.linglevel.api.crawling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDslResponse {
    
    @Schema(description = "생성된 DSL ID", example = "60d0fe4f5311236168a109ca")
    @JsonProperty("_id")
    private String id;
    
    @Schema(description = "도메인명", example = "coupang.com")
    private String domain;
    
    @Schema(description = "응답 메시지", example = "DSL created successfully.")
    private String message;
}