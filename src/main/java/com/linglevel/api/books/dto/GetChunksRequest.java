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
@Schema(description = "청크 목록 조회 요청")
public class GetChunksRequest {
    
    @Schema(description = "난이도 레벨", 
            example = "a1", 
            allowableValues = {"a0", "a1", "a2", "b1", "b2", "c1", "c2"},
            required = true)
    private String difficulty;
    
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
} 