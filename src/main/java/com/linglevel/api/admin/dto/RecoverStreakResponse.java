package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스트릭 복구 응답")
public class RecoverStreakResponse {

    @Schema(description = "응답 메시지", example = "Streak recovered successfully.")
    private String message;

    @Schema(description = "사용자 ID", example = "user123")
    private String userId;

    @Schema(description = "복구 시작 날짜", example = "2025-01-10")
    private LocalDate startDate;

    @Schema(description = "복구 종료 날짜", example = "2025-01-15")
    private LocalDate endDate;

    @Schema(description = "복구 후 현재 스트릭", example = "15")
    private Integer currentStreak;

    @Schema(description = "복구 후 최장 스트릭", example = "20")
    private Integer longestStreak;

    @Schema(description = "마지막 완료일", example = "2025-01-15")
    private LocalDate lastCompletionDate;
}
