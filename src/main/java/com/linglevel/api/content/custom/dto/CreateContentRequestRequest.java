package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.entity.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "콘텐츠 처리 요청 생성 요청")
public class CreateContentRequestRequest {
    
    @Schema(description = "콘텐츠 제목", example = "My Custom Article")
    private String title;
    
    @Schema(description = "콘텐츠 타입", example = "TEXT")
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    private ContentType contentType;
    
    @Schema(description = "처리할 원본 텍스트 (최대 10,000자)", 
            example = "Once upon a time, there was a little prince who lived on a small planet...")
    @NotBlank(message = "원본 콘텐츠는 필수입니다.")
    @Size(min=100, max = 15000, message = "원본 콘텐츠는 최소 100자부터 최대 15,000자까지 입력 가능합니다.")
    private String originalContent;
    
    @Schema(description = "목표 난이도 목록", example = "[\"A1\", \"B1\"]")
    private List<DifficultyLevel> targetDifficultyLevels;
    
    @Schema(description = "원본 링크 URL (링크 타입인 경우)", example = "https://example.com/article")
    private String originUrl;

    @Schema(description = "원본 저자", example = "작가명")
    private String originAuthor;

    @Schema(description = "커버 이미지 URL (옵셔널)", example = "https://example.com/image.jpg")
    private String coverImageUrl;
}