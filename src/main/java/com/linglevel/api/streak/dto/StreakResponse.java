package com.linglevel.api.streak.dto;

import com.linglevel.api.streak.entity.StreakStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스트릭 정보 응답")
public class StreakResponse {

    @Schema(description = "현재 연속 일수", example = "25")
    private Integer currentStreak;

    @Schema(description = "오늘의 스트릭 상태 (COMPLETED, FREEZE_USED, MISSED)", example = "COMPLETED")
    private StreakStatus todayStatus;

    @Schema(description = "최장 스트릭 기록", example = "30")
    private Integer longestStreak;

    @Schema(description = "현재 스트릭 시작 날짜 (yyyy-MM-dd, KST)", example = "2025-10-01")
    private LocalDate streakStartDate;

    @Schema(description = "전체 학습일 수", example = "45")
    private Long totalStudyDays;

    @Schema(description = "읽은 콘텐츠 총 개수", example = "120")
    private Long totalContentsRead;

    @Schema(description = "보유 프리즈 개수", example = "2")
    private Integer freezeCount;

    @Schema(description = "총 누적 학습 시간 (초)", example = "36000")
    private Long totalReadingTimeSeconds;

    @Schema(description = "상위 몇 퍼센트인지 (전체 유저 중)", example = "5")
    private Integer percentileRank;

    @Schema(description = "격려 메시지")
    private EncouragementMessage encouragementMessage;
}
