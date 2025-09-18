package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "AI 콘텐츠 처리 완료 웹훅 요청")
public class CustomContentCompletedRequest {
    
    @Schema(description = "처리 요청의 고유 ID", example = "60d0fe4f5311236168a109ca", required = true)
    @NotBlank(message = "요청 ID는 필수입니다.")
    private String requestId;
}