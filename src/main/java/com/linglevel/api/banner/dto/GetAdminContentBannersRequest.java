package com.linglevel.api.banner.dto;

import com.linglevel.api.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Schema(description = "관리자용 콘텐츠 배너 목록 조회 요청")
public class GetAdminContentBannersRequest {

    @Schema(description = "국가 코드로 필터링", example = "KR")
    private CountryCode countryCode;

    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    @Min(value = 1, message = "Page must be at least 1")
    private Integer page = 1;

    @Schema(description = "페이지 당 항목 수", example = "10", defaultValue = "10")
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    private Integer limit = 10;
}