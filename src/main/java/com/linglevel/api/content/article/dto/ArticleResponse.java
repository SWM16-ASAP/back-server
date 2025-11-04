package com.linglevel.api.content.article.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.i18n.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "기사 응답")
public class ArticleResponse {
    
    @Schema(description = "기사 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "기사 제목", example = "Viking King's Bizarre Legacy: The Shocking Truth Behind Your Phone's Most Mysterious Feature!")
    private String title;
    
    @Schema(description = "작가", example = "")
    private String author;
    
    @Schema(description = "커버 이미지 URL", example = "https://path/to/cover.jpg")
    private String coverImageUrl;
    
    @Schema(description = "난이도 레벨", example = "C1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "청크 개수", example = "15")
    private Integer chunkCount;

    @Schema(description = "현재 읽은 청크 번호", example = "7")
    private Integer currentReadChunkNumber;

    @Schema(description = "진행률", example = "46.7")
    private Double progressPercentage;

    @Schema(description = "현재 선택한 난이도", example = "EASY")
    private DifficultyLevel currentDifficultyLevel;

    @Schema(description = "완료 여부", example = "false")
    private Boolean isCompleted;
    
    @Schema(description = "읽기 시간(분)", example = "8")
    private Integer readingTime;
    
    @Schema(description = "평균 평점", example = "4.5")
    private Double averageRating;
    
    @Schema(description = "리뷰 개수", example = "230")
    private Integer reviewCount;
    
    @Schema(description = "조회수", example = "15000")
    private Integer viewCount;

    @Schema(description = "카테고리", example = "TECH")
    private ContentCategory category;

    @Schema(description = "태그 목록", example = "[\"technology\", \"history\"]")
    private List<String> tags;

    @Schema(description = "타깃 언어 코드 목록", example = "[\"KO\", \"EN\", \"JA\"]")
    private List<LanguageCode> targetLanguageCode;

    @Schema(description = "생성 날짜", example = "2024-01-15T00:00:00Z")
    private Instant createdAt;
}