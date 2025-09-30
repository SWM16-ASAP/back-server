package com.linglevel.api.content.article.dto;

import com.linglevel.api.content.common.ProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetArticlesRequest {

    @Schema(description = "정렬 기준", example = "created_at", defaultValue = "created_at", allowableValues = {"view_count", "average_rating", "created_at"})
    private String sortBy = "created_at";

    @Schema(description = "태그 필터 (쉼표로 구분)", example = "technology,business")
    private String tags;

    @Schema(description = "키워드 검색", example = "viking")
    private String keyword;

    @Schema(description = "진도별 필터링", example = "IN_PROGRESS")
    private ProgressStatus progress;

    @Schema(description = "페이지 번호", example = "1", defaultValue = "1", minimum = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10", minimum = "1", maximum = "200")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
    @Max(value = 200, message = "페이지 당 항목 수는 200 이하여야 합니다.")
    private Integer limit = 10;
}