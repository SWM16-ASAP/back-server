package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "푸시 알림 오픈 리포트 요청")
public class PushOpenedRequest {

    @NotBlank(message = "Campaign ID is required")
    @Schema(description = "캠페인 ID", example = "article-tech-1234567890", required = true)
    private String campaignId;

    @NotBlank(message = "User ID is required")
    @Schema(description = "사용자 ID", example = "user123", required = true)
    private String userId;

    @NotNull(message = "Opened time is required")
    @Schema(description = "알림을 오픈한 시간", example = "2024-01-15T10:30:00", required = true)
    private LocalDateTime openedAt;
}
