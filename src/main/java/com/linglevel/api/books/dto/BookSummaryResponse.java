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
@Schema(description = "책 요약 정보 응답")
public class BookSummaryResponse {
    @Schema(description = "책 ID", example = "60d0fe4f5311236168a109cb")
    private String id;
    
    @Schema(description = "책 제목", example = "The Little Prince")
    private String title;
    
    @Schema(description = "저자", example = "Antoine de Saint-Exupéry")
    private String author;
    
    @Schema(description = "표지 이미지 URL", example = "https://path/to/cover.jpg")
    private String coverImageUrl;
    
    @Schema(description = "총 챕터 수", example = "27")
    private Integer totalChapters;
} 