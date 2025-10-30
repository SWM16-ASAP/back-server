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
@Schema(description = "달력 응답")
public class CalendarResponse {

    @Schema(description = "년도", example = "2025")
    private Integer year;

    @Schema(description = "월", example = "10")
    private Integer month;

    @Schema(description = "오늘 일자", example = "24")
    private Integer today;

    @Schema(description = "현재 스트릭", example = "25")
    private Integer currentStreak;

    @Schema(description = "달력 일자별 정보 목록")
    private List<CalendarDayResponse> days;
}
