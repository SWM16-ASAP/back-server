package com.linglevel.api.banner.dto;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema(description = "콘텐츠 배너 생성 요청")
public class CreateContentBannerRequest {

    @Schema(description = "국가 코드", example = "KR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "CountryCode is required.")
    private CountryCode countryCode;

    @Schema(description = "콘텐츠 ID", example = "60d0fe4f5311236168a109cb", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ContentId is required.")
    private String contentId;

    @Schema(description = "콘텐츠 타입", example = "BOOK", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ContentType is required.")
    private ContentType contentType;

    @Schema(description = "배너 부제목", example = "세계에서 가장 사랑받는 소설")
    private String subtitle;

    @Schema(description = "배너 제목", example = "어린왕자와 함께하는 영어 공부", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title is required.")
    private String title;

    @Schema(description = "배너 설명", example = "프랑스 문학의 걸작을 쉬운 영어로 만나보세요. A1부터 C2까지 다양한 난이도로 제공됩니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Description is required.")
    private String description;

    @Schema(description = "표시 순서", example = "1", defaultValue = "9")
    private Integer displayOrder = 9;

    @Schema(description = "활성화 상태", example = "true", defaultValue = "true")
    private Boolean isActive = true;
}