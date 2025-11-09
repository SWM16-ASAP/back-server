package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "콘텐츠 처리 요청 생성 응답")
public class CreateContentRequestResponse {
    
    @Schema(description = "생성된 요청 ID", example = "60d0fe4f5311236168a109ca")
    private String requestId;
    
    @Schema(description = "제목", example = "My Custom Article")
    private String title;
    
    @Schema(description = "요청 상태", example = "PENDING")
    private String status;

    @Schema(description = "캐시 히트 여부", example = "true")
    private boolean cached;

    @Schema(description = "캐시된 커스텀 콘텐츠 ID", example = "60d0fe4f5311236162332939")
    private String customContentId;

    @Schema(description = "캐시된 커스텀 콘텐츠 타이틀", example = "Article Real Title")
    private String customContentTitle;

    @Schema(description = "생성일시", example = "2024-01-15T10:00:00Z")
    private Instant createdAt;
}