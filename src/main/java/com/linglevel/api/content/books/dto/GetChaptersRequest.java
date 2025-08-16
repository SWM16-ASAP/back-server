package com.linglevel.api.content.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챕터 목록 조회 요청")
public class GetChaptersRequest {
    
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