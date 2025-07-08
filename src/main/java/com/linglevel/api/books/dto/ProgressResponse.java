package com.linglevel.api.books.dto;

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
@Schema(description = "읽기 진도 정보 응답")
public class ProgressResponse {
    @Schema(description = "진도 ID", example = "60d0fe4f5311236168a109d1")
    private String _id;
    
    @Schema(description = "책 ID", example = "60d0fe4f5311236168a109cb")
    private String bookId;
    
    @Schema(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
    private String chapterId;
    
    @Schema(description = "총 청크 수", example = "30")
    private Integer totalChunks;
    
    @Schema(description = "현재 읽은 청크 번호", example = "5")
    private Integer currentReadChunkNumber;
    
    @Schema(description = "업데이트 일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
} 