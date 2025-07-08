package com.linglevel.api.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 페이지네이션 응답")
public class PageResponseDTO<T> {
    @Schema(description = "응답 데이터")
    private List<T> data;
    
    @Schema(description = "현재 페이지", example = "1")
    private int currentPage;
    
    @Schema(description = "전체 페이지", example = "10")
    private int totalPages;
    
    @Schema(description = "전체 항목 수", example = "100")
    private int totalCount;
    
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
    
    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;
    
    public static <T> PageResponseDTO<T> of(List<T> data, int currentPage, int totalPages, int totalCount) {
        return PageResponseDTO.<T>builder()
                .data(data)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalCount(totalCount)
                .hasNext(currentPage < totalPages)
                .hasPrevious(currentPage > 1)
                .build();
    }
} 