package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "콘텐츠 처리 요청 응답")
public class ContentRequestResponse {
    
    @Schema(description = "요청 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "제목", example = "My Custom Article")
    private String title;

    @Schema(description = "원본 텍스트")
    private String originalText;
    
    @Schema(description = "콘텐츠 타입", example = "CLIPBOARD")
    private String contentType;
    
    @Schema(description = "목표 난이도 목록", example = "[\"A1\", \"B1\"]")
    private List<DifficultyLevel> targetDifficultyLevels;
    
    @Schema(description = "원본 URL", example = "https://example.com/article")
    private String originUrl;
    
    @Schema(description = "원본 도메인", example = "example.com")
    private String originDomain;
    
    @Schema(description = "원본 저자", example = "작가명")
    private String originAuthor;
    
    @Schema(description = "처리 상태", example = "PROCESSING")
    private String status;
    
    @Schema(description = "진행률 (0-100)", example = "45")
    private Integer progress;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "완료일시", example = "2024-01-15T10:05:00")
    private LocalDateTime completedAt;
    
    @Schema(description = "에러 메시지", example = "error_message")
    private String errorMessage;
    
    @Schema(description = "결과 커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109cc")
    private String resultCustomContentId;
}