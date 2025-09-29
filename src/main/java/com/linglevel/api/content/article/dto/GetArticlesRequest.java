package com.linglevel.api.content.article.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetArticlesRequest {
    
    @Parameter(description = "정렬 기준", example = "created_at")
    private String sortBy = "created_at";
    
    @Parameter(description = "태그 필터 (쉼표로 구분)", example = "technology,business")
    private String tags;
    
    @Parameter(description = "키워드 검색", example = "viking")
    private String keyword;

    @Parameter(description = "진도별 필터링", example = "in_progress")
    private String progress;
    
    @Parameter(description = "페이지 번호", example = "1")
    private Integer page = 1;
    
    @Parameter(description = "페이지당 항목 수", example = "10")
    private Integer limit = 10;
}