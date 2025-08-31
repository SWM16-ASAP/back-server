package com.linglevel.api.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "앱 버전 업데이트 응답")
public class VersionUpdateResponse {
    @Schema(description = "최신 버전", example = "1.2.3")
    private String latestVersion;
    
    @Schema(description = "최소 요구 버전", example = "1.1.0")
    private String minimumVersion;
    
    @Schema(description = "업데이트 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
}