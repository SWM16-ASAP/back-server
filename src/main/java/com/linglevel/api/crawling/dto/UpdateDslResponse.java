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
public class UpdateDslResponse {
    
    @Schema(description = "업데이트된 DSL ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "도메인명", example = "coupang.com")
    private String domain;

    @Schema(description = "사이트명", example = "쿠팡")
    private String name;

    @Schema(description = "업데이트된 제목 추출 DSL 규칙")
    private String titleDsl;
    
    @Schema(description = "업데이트된 본문 추출 DSL 규칙")
    private String contentDsl;

    @Schema(description = "업데이트된 섬네일 추출 DSL 규칙")
    private String thumbnailDsl;

    @Schema(description = "응답 메시지", example = "DSL updated successfully.")
    private String message;
}