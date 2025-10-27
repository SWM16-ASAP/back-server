package com.linglevel.api.streak.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Content completion information DTO
 * Used to transfer content completion data from Progress services to StreakService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "콘텐츠 완료 정보")
public class ContentInfo {

    @Schema(description = "콘텐츠 타입 (BOOK, ARTICLE, CUSTOM)", example = "BOOK", required = true)
    private ContentType type;

    @Schema(description = "콘텐츠 ID", example = "60d5ec49f1b2c8a5d8e4f123", required = true)
    private String contentId;

    @Schema(description = "챕터 ID (책의 경우만 해당)", example = "60d5ec49f1b2c8a5d8e4f456")
    private String chapterId;

    @Schema(description = "완료 시각", example = "2025-10-27T10:30:00Z", required = true)
    private Instant completedAt;

    @Schema(description = "읽는데 걸린 시간 (초 단위)", example = "180", required = true)
    private Integer readingTime;

    @Schema(description = "콘텐츠 카테고리", example = "TECH", required = true)
    private ContentCategory category;

    @Schema(description = "난이도", example = "B1", required = true)
    private DifficultyLevel difficultyLevel;
}
