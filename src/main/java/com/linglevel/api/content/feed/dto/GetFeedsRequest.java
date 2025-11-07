package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.FeedContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetFeedsRequest {

    @Schema(description = "콘텐츠 타입 필터 (여러 개 선택 가능, null이면 전체)", example = "[\"YOUTUBE\", \"BLOG\"]")
    private List<FeedContentType> contentTypes;

    @Schema(description = "카테고리 필터 (단일 선택, null이면 전체)", example = "TECH")
    private ContentCategory category;

    @Schema(description = "정렬 기준", example = "LATEST", defaultValue = "LATEST")
    private SortOrder sortOrder = SortOrder.LATEST;

    @Schema(description = "페이지 번호", example = "1", defaultValue = "1", minimum = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    @Schema(description = "페이지 당 항목 수", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
    @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 당 항목 수는 100 이하여야 합니다.")
    private Integer limit = 20;

    public enum SortOrder {
        RECOMMENDED,  // 추천순 (사용자 선호도 기반)
        LATEST,       // 최신순
        POPULAR       // 인기순 (조회수 기반)
    }
}
