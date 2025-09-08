package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "콘텐츠 처리 요청 목록 조회 요청")
public class GetContentRequestsRequest {
    
    @Schema(description = "상태별 필터링", example = "COMPLETED", 
            allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"})
    private String status;
    
    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    private Integer limit = 10;
}