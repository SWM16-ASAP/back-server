package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 읽기 진도 정보 응답")
public class BooksProgressResponse {
    @Schema(description = "진도 ID", example = "60d0fe4f5311236168a109d1")
    private String id;
    
    @Schema(description = "책 정보")
    private BookSummaryResponse book;
    
    @Schema(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
    private String chapterId;
    
    @Schema(description = "청크 ID", example = "60d0fe4f53112389248a182db")
    private String chunkId;
    
    @Schema(description = "현재 읽은 챕터 번호", example = "1")
    private Integer currentReadChapterNumber;
    
    @Schema(description = "챕터 진행률", example = "15.5")
    private Double progressPercentage;
    
    @Schema(description = "업데이트 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
} 