package com.linglevel.api.version.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "앱 버전 정보 응답")
public class VersionResponse {
    @Schema(description = "최신 버전", example = "1.2.3")
    private String latestVersion;
    
    @Schema(description = "최소 요구 버전", example = "1.1.0")
    private String minimumVersion;
}