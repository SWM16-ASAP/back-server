package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.entity.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema(description = "콘텐츠 처리 요청 생성 요청")
public class CreateContentRequestRequest {
    
    @Schema(description = "콘텐츠 제목", example = "My Custom Article")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    
    @Schema(description = "콘텐츠 타입", example = "TEXT", allowableValues = {"TEXT", "LINK", "PDF"})
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    private ContentType contentType;
    
    @Schema(description = "처리할 원본 텍스트", 
            example = "Once upon a time, there was a little prince who lived on a small planet...")
    @NotBlank(message = "원본 콘텐츠는 필수입니다.")
    private String originalContent;
    
    @Schema(description = "목표 난이도", example = "A1")
    private DifficultyLevel targetDifficultyLevel;
    
    @Schema(description = "원본 링크 URL (링크 타입인 경우)", example = "https://example.com/article")
    private String originUrl;
    
    @Schema(description = "원본 저자", example = "작가명")
    private String originAuthor;
}