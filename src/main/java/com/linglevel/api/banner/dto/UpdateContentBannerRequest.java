package com.linglevel.api.banner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "콘텐츠 배너 수정 요청")
public class UpdateContentBannerRequest {

    @Schema(description = "배너 제목", example = "업데이트된 배너 제목")
    private String title;

    @Schema(description = "배너 부제목", example = "업데이트된 부제목")
    private String subTitle;

    @Schema(description = "배너 설명", example = "업데이트된 설명")
    private String description;

    @Schema(description = "표시 순서", example = "2")
    private Integer displayOrder;

    @Schema(description = "활성화 상태", example = "false")
    private Boolean isActive;
}