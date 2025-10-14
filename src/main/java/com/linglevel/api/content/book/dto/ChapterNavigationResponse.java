package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챕터 네비게이션 정보 응답")
public class ChapterNavigationResponse {
    @Schema(description = "현재 챕터 ID", example = "60d0fe4f5311236168a109cb")
    private String currentChapterId;

    @Schema(description = "현재 챕터 번호", example = "5")
    private Integer currentChapterNumber;

    @Schema(description = "이전 챕터 존재 여부", example = "true")
    private Boolean hasPreviousChapter;

    @Schema(description = "이전 챕터 ID", example = "60d0fe4f5311236168a109ca")
    private String previousChapterId;

    @Schema(description = "다음 챕터 존재 여부", example = "true")
    private Boolean hasNextChapter;

    @Schema(description = "다음 챕터 ID", example = "60d0fe4f5311236168a109cc")
    private String nextChapterId;
}