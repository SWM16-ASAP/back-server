package com.linglevel.api.word.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "단어 목록 조회 요청")
public class GetWordsRequest {
    
    @Schema(description = "페이지 번호", 
            example = "1", 
            minimum = "1",
            defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "페이지 크기", 
            example = "10", 
            minimum = "1", 
            maximum = "50",
            defaultValue = "10")
    private Integer limit = 10;
    
    @Schema(description = "검색할 단어 (부분 일치 검색)", 
            example = "magn")
    private String search;
}