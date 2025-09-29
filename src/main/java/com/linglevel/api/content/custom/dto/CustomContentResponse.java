package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "커스텀 콘텐츠 응답")
public class CustomContentResponse {
    
    @Schema(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "제목", example = "My Custom Article")
    private String title;
    
    @Schema(description = "작가", example = "AI Generated")
    private String author;
    
    @Schema(description = "커버 이미지 URL", example = "https://path/to/cover.jpg")
    private String coverImageUrl;
    
    @Schema(description = "AI가 분석한 최종 난이도", example = "A1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "사용자가 요청한 목표 난이도 목록", example = "[\"A1\", \"B1\"]")
    private List<DifficultyLevel> targetDifficultyLevels;
    
    @Schema(description = "청크 개수", example = "12")
    private Integer chunkCount;

    @Schema(description = "현재 읽은 청크 번호", example = "7")
    private Integer currentReadChunkNumber;

    @Schema(description = "진행률", example = "58.3")
    private Double progressPercentage;

    @Schema(description = "완료 여부", example = "false")
    private Boolean isCompleted;

    @Schema(description = "예상 읽기 시간(분)", example = "8")
    private Integer readingTime;
    
    @Schema(description = "평균 평점", example = "4.2")
    private Double averageRating;
    
    @Schema(description = "리뷰 개수", example = "15")
    private Integer reviewCount;
    
    @Schema(description = "조회수", example = "150")
    private Integer viewCount;
    
    @Schema(description = "태그 목록", example = "[\"technology\", \"beginner\"]")
    private List<String> tags;
    
    @Schema(description = "원본 URL", example = "https://example.com/article")
    private String originUrl;
    
    @Schema(description = "출처 도메인", example = "example.com")
    private String originDomain;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:05:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T10:05:00")
    private LocalDateTime updatedAt;
}