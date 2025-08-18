package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private String sortBy = "created_at";
    
    @Schema(description = "태그 필터 (쉼표로 구분)", 
            example = "philosophy,children")
    private String tags;
    
    @Schema(description = "검색 키워드 (제목 또는 작가명)", 
            example = "prince")
    private String keyword;
    
    @Schema(description = "페이지 번호", 
            example = "1", 
            minimum = "1",
            defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "페이지 크기", 
            example = "10", 
            minimum = "1", 
            maximum = "100",
            defaultValue = "10")
    private Integer limit = 10;
} 