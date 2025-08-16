package com.linglevel.api.content.news.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetNewsChunksRequest {
    
    @Parameter(description = "난이도 레벨", example = "a1", required = true)
    private String difficulty;
    
    @Parameter(description = "페이지 번호", example = "1")
    private Integer page = 1;
    
    @Parameter(description = "페이지당 항목 수", example = "10")
    private Integer limit = 10;
}