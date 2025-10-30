package com.linglevel.api.streak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주간 스트릭 응답")
public class WeekStreakResponse {

    @Schema(description = "현재 스트릭", example = "25")
    private Integer currentStreak;

    @Schema(description = "보유한 프리즈 개수", example = "2")
    private Integer freezeCount;

    @Schema(description = "이번 주 요일별 정보 목록 (월요일부터 시작)")
    private List<WeekDayResponse> weekDays;
}
