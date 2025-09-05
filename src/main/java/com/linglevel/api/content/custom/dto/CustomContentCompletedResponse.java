package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 콘텐츠 처리 완료 웹훅 응답")
public class CustomContentCompletedResponse {
    
    @Schema(description = "요청 ID", example = "60d0fe4f5311236168a109ca")
    private String requestId;
    
    @Schema(description = "처리 상태", example = "completed")
    private String status;
}