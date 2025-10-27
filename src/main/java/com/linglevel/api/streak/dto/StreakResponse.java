package com.linglevel.api.streak.dto;

import com.linglevel.api.streak.entity.StreakStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "스트릭 정보 응답")
public class StreakResponse {

    @Schema(description = "현재 연속 일수", example = "25")
    private int currentStreak;

    @Schema(description = "오늘의 스트릭 상태 (COMPLETED, FREEZE_USED, MISSED)")
    private StreakStatus todayStatus;

    @Schema(description = "최장 연속 일수", example = "30")
    private int longestStreak;

    @Schema(description = "현재 스트릭 시작 날짜 (yyyy-MM-dd, KST)", example = "2025-10-01")
    private LocalDate streakStartDate;

    @Schema(description = "전체 학습일 (dailyCompletions count)", example = "45")
    private long totalStudyDays;

    @Schema(description = "읽은 고유 콘텐츠 개수 (dailyCompletions.firstCompletionCount의 합)", example = "120")
    private long totalContentsRead;

    @Schema(description = "사용 가능한 프리즈 개수", example = "2")
    private int availableFreezes;

    @Schema(description = "총 누적 학습 시간 (초)", example = "36000")
    private long totalReadingTimeSeconds;

    @Schema(description = "상위 몇% (전체 유저 중 currentStreak 기준)", example = "5.0")
    private double percentile;

    @Schema(description = "격려 메시지")
    private EncouragementMessage encouragementMessage;
}