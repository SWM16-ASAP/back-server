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
@Schema(description = "나의 읽기 진도 조회 요청")
public class GetBooksProgressRequest {
    
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