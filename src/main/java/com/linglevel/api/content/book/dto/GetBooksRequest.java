package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.linglevel.api.content.common.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "책 목록 조회 요청")
public class GetBooksRequest {
    
    @Schema(description = "정렬 기준",
            example = "created_at",
            allowableValues = {"view_count", "average_rating", "created_at"},
            defaultValue = "created_at")
    @Builder.Default
    private String sortBy = "created_at";
    
    @Schema(description = "태그 필터 (쉼표로 구분)", 
            example = "philosophy,children")
    private String tags;
    
    @Schema(description = "검색 키워드 (제목 또는 작가명)",
            example = "prince")
    private String keyword;

    @Schema(description = "진도별 필터링", example = "IN_PROGRESS")
    private ProgressStatus progress;
    
    @Schema(description = "페이지 번호",
            example = "1",
            minimum = "1",
            defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    @Builder.Default
    private Integer page = 1;
    
    @Schema(description = "페이지 크기",
            example = "10",
            minimum = "1",
            maximum = "200",
            defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 200, message = "페이지 크기는 200 이하여야 합니다.")
    @Builder.Default
    private Integer limit = 10;
} 