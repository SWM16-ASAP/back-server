package com.linglevel.api.streak.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.linglevel.api.streak.entity.StreakStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "달력 일자별 정보")
public class CalendarDayResponse {

    @Schema(description = "날짜 (yyyy-MM-dd)", example = "2025-10-01")
    private LocalDate date;

    @Schema(description = "일자", example = "1")
    private Integer dayOfMonth;

    @Schema(description = "오늘 여부", example = "false")
    private Boolean isToday;

    @Schema(description = "스트릭 상태 (COMPLETED: 완료, FREEZE_USED: 프리즈 사용, MISSED: 놓침, FUTURE: 미래)",
            example = "COMPLETED")
    private StreakStatus status;

    @Schema(description = "해당 날짜의 스트릭 개수 (FUTURE인 경우 null)", example = "20")
    private Integer streakCount;

    @Schema(description = "해당 날짜에 첫 완료한 고유 콘텐츠 개수 (복습 제외, FUTURE인 경우 null)", example = "2")
    private Integer firstCompletionCount;

    @Schema(description = "해당 날짜에 완료한 총 콘텐츠 개수 (복습 포함, FUTURE인 경우 null)", example = "3")
    private Integer totalCompletionCount;

    @Schema(description = "획득한 보상 정보 (FUTURE가 아닌 경우)")
    private RewardInfo rewards;

    @Schema(description = "예상 보상 정보 (FUTURE인 경우)")
    private RewardInfo expectedRewards;
}
