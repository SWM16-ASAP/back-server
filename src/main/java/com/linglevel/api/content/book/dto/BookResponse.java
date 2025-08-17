package com.linglevel.api.content.book.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "책 정보 응답")
public class BookResponse {
    @Schema(description = "책 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "책 제목", example = "The Little Prince")
    private String title;
    
    @Schema(description = "저자", example = "Antoine de Saint-Exupéry")
    private String author;
    
    @Schema(description = "표지 이미지 URL", example = "https://path/to/cover.jpg")
    private String coverImageUrl;
    
    @Schema(description = "기본 난이도", example = "A1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "총 챕터 수", example = "27")
    private Integer chapterCount;
    
    @Schema(description = "현재 읽은 챕터 번호", example = "10")
    private Integer currentReadChapterNumber;
    
    @Schema(description = "진행률", example = "37.0")
    private Double progressPercentage;
    
    @Schema(description = "읽기 시간(분)", example = "120")
    private Integer readingTime;
    
    @Schema(description = "평균 평점", example = "4.8")
    private Double averageRating;
    
    @Schema(description = "리뷰 수", example = "1500")
    private Integer reviewCount;
    
    @Schema(description = "조회수", example = "25000")
    private Integer viewCount;
    
    @Schema(description = "태그 목록", example = "[\"philosophy\", \"children\"]")
    private List<String> tags;
    
    @Schema(description = "생성일자", example = "2024-01-15T00:00:00")
    private LocalDateTime createdAt;
} 