package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "AI 콘텐츠 처리 완료 웹훅 응답")
public class CustomContentCompletedResponse {
    
    @Schema(description = "생성된 커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
}