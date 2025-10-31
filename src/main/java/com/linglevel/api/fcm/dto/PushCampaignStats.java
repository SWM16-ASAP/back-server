package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "푸시 캠페인 통계")
public class PushCampaignStats {

    @Schema(description = "캠페인 ID", example = "article-tech-1234567890")
    private String campaignId;

    @Schema(description = "캠페인 시작 시간 (첫 전송 시간)", example = "2024-01-15T10:00:00")
    private LocalDateTime firstSentAt;

    @Schema(description = "총 전송 시도 수", example = "1000")
    private int totalSent;

    @Schema(description = "전송 성공 수", example = "950")
    private int sentSuccess;

    @Schema(description = "알림 오픈 수", example = "380")
    private int totalOpened;

    @Schema(description = "전송 성공률 (sentSuccess / totalSent)", example = "0.95")
    private double deliveryRate;

    @Schema(description = "오픈율 (totalOpened / sentSuccess)", example = "0.40")
    private double openRate;
}
