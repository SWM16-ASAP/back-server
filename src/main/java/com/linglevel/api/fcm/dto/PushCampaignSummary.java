package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "푸시 캠페인 요약")
public class PushCampaignSummary {

    @Schema(description = "캠페인 ID", example = "article-tech-1234567890")
    private String campaignId;

    @Schema(description = "캠페인 타입 (campaignId에서 추출)", example = "article")
    private String campaignType;

    @Schema(description = "캠페인 시작 시간", example = "2024-01-15T10:00:00")
    private LocalDateTime firstSentAt;

    @Schema(description = "총 전송 수", example = "1000")
    private int totalSent;

    @Schema(description = "전송 성공 수", example = "950")
    private int sentSuccess;

    @Schema(description = "오픈 수", example = "380")
    private int totalOpened;

    @Schema(description = "오픈율", example = "0.40")
    private double openRate;
}
