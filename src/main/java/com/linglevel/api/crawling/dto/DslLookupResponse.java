package com.linglevel.api.crawling.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DslLookupResponse {
    
    @Schema(description = "도메인명", example = "coupang.com")
    private String domain;
    
    @Schema(description = "제목 추출 DSL 규칙 (validate_only=true인 경우 포함되지 않음)")
    private String titleDsl;
    
    @Schema(description = "본문 추출 DSL 규칙 (validate_only=true인 경우 포함되지 않음)")
    private String contentDsl;

    @Schema(description = "섬네일 추출 DSL 규칙 (validate_only=true인 경우 포함되지 않음)")
    private String thumbnailDsl;

    @Schema(description = "유효성 여부", example = "true")
    private boolean valid;
    
    @Schema(description = "응답 메시지")
    private String message;
}