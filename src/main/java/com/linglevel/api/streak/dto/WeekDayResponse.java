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
@Schema(description = "주간 일자별 정보")
public class WeekDayResponse {

    @Schema(description = "요일 (MON, TUE, WED, THU, FRI, SAT, SUN)", example = "MON")
    private String dayOfWeek;

    @Schema(description = "날짜 (yyyy-MM-dd)", example = "2025-10-20")
    private LocalDate date;

    @Schema(description = "오늘 여부", example = "false")
    private Boolean isToday;

    @Schema(description = "스트릭 상태 (COMPLETED: 완료, FREEZE_USED: 프리즈 사용, MISSED: 놓침, FUTURE: 미래)",
            example = "COMPLETED")
    private StreakStatus status;

    @Schema(description = "획득한 보상 정보 (FUTURE가 아닌 경우)")
    private RewardInfo rewards;

    @Schema(description = "예상 보상 정보 (FUTURE인 경우)")
    private RewardInfo expectedRewards;
}
