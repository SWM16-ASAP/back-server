package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스트릭 복구 요청")
public class RecoverStreakRequest {

    @NotBlank(message = "userId는 필수입니다.")
    @Schema(description = "사용자 ID", example = "user123", required = true)
    private String userId;

    @NotNull(message = "startDate는 필수입니다.")
    @Schema(description = "복구 시작 날짜 (YYYY-MM-DD)", example = "2025-01-10", required = true)
    private LocalDate startDate;

    @NotNull(message = "endDate는 필수입니다.")
    @Schema(description = "복구 종료 날짜 (YYYY-MM-DD)", example = "2025-01-15", required = true)
    private LocalDate endDate;
}
