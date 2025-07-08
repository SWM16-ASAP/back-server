package com.linglevel.api.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챕터 정보 응답")
public class ChapterResponse {
    @Schema(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
    private String id;
    
    @Schema(description = "챕터 번호", example = "1")
    private Integer chapterNumber;
    
    @Schema(description = "챕터 제목", example = "The Drawing")
    private String title;
    
    @Schema(description = "챕터 이미지 URL", example = "https://path/to/chapter-image.jpg")
    private String chapterImageUrl;
    
    @Schema(description = "챕터 설명", example = "A brief summary of the first chapter.")
    private String description;
    
    @Schema(description = "총 청크 수", example = "10")
    private Integer chunkCount;
    
    @Schema(description = "현재 읽은 청크 번호", example = "8")
    private Integer currentReadChunkNumber;
    
    @Schema(description = "진행률", example = "80.0")
    private Double progressPercentage;
    
    @Schema(description = "읽기 시간(분)", example = "15")
    private Integer readingTime;
} 