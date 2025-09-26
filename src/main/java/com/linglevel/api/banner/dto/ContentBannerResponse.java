package com.linglevel.api.banner.dto;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "콘텐츠 배너 응답")
public class ContentBannerResponse {

    @Schema(description = "배너 ID", example = "60d0fe4f5311236168a109ca")
    private String id;

    @Schema(description = "국가 코드", example = "KR")
    private CountryCode countryCode;

    @Schema(description = "콘텐츠 ID", example = "60d0fe4f5311236168a109cb")
    private String contentId;

    @Schema(description = "콘텐츠 타입", example = "BOOK")
    private ContentType contentType;

    @Schema(description = "콘텐츠 제목", example = "The Little Prince")
    private String contentTitle;

    @Schema(description = "콘텐츠 작가", example = "Antoine de Saint-Exupéry")
    private String contentAuthor;

    @Schema(description = "콘텐츠 커버 이미지 URL", example = "https://path/to/cover.jpg")
    private String contentCoverImageUrl;

    @Schema(description = "콘텐츠 읽기 시간(분)", example = "120")
    private Integer contentReadingTime;

    @Schema(description = "배너 부제목", example = "세계에서 가장 사랑받는 소설")
    private String subtitle;

    @Schema(description = "배너 제목", example = "어린왕자와 함께하는 영어 공부")
    private String title;

    @Schema(description = "배너 설명", example = "프랑스 문학의 걸작을 쉬운 영어로 만나보세요. A1부터 C2까지 다양한 난이도로 제공됩니다.")
    private String description;

    @Schema(description = "표시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "활성화 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "생성 날짜", example = "2024-01-15T00:00:00")
    private LocalDateTime createdAt;
}