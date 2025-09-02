package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "AI 콘텐츠 처리 완료 웹훅 요청")
public class CustomContentCompletedRequest {
    
    @Schema(description = "S3에 저장된 JSON 파일의 식별자", example = "string", required = true)
    @NotBlank(message = "ID는 필수입니다.")
    private String id;
}