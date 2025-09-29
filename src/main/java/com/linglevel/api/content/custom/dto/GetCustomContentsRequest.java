package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "커스텀 콘텐츠 목록 조회 요청")
public class GetCustomContentsRequest {
    
    @Schema(description = "정렬 기준", example = "created_at", defaultValue = "created_at",
            allowableValues = {"view_count", "average_rating", "created_at"})
    private String sortBy = "created_at";
    
    @Schema(description = "검색할 태그들 (쉼표로 구분)", example = "technology,beginner")
    private String tags;
    
    @Schema(description = "검색할 콘텐츠 제목 또는 작가 이름", example = "prince")
    private String keyword;

    @Schema(description = "진도별 필터링", example = "in_progress",
            allowableValues = {"not_started", "in_progress", "completed"})
    private String progress;

    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    private Integer page = 1;
    
    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    private Integer limit = 10;
}