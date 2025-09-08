package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema(description = "AI 콘텐츠 처리 진행률 웹훅 요청")
public class CustomContentProgressRequest {
    
    @Schema(description = "처리 요청의 고유 ID", example = "60d0fe4f5311236168a109ca", required = true)
    @NotBlank(message = "요청 ID는 필수입니다.")
    private String requestId;
    
    @Schema(description = "진행률 0-100", example = "75", required = true)
    @NotNull(message = "진행률은 필수입니다.")
    @Min(value = 0, message = "진행률은 0 이상이어야 합니다.")
    @Max(value = 100, message = "진행률은 100 이하여야 합니다.")
    private Integer progress;
}