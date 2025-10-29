package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetTodayStreakRequest {

    @Schema(description = "오늘의 스트릭을 리셋할 사용자의 ID", example = "60d5ec49f1b2c8a5d8e4f123", required = true)
    @NotBlank(message = "userId는 필수입니다.")
    private String userId;
}
