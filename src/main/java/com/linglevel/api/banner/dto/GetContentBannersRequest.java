package com.linglevel.api.banner.dto;

import com.linglevel.api.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Schema(description = "콘텐츠 배너 목록 조회 요청")
public class GetContentBannersRequest {

    @Schema(description = "국가 코드 (필수)", example = "KR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "CountryCode is required.")
    private CountryCode countryCode;

    @Schema(description = "반환할 배너 수", example = "5", defaultValue = "5")
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 10, message = "Limit cannot exceed 10")
    private Integer limit = 5;
}