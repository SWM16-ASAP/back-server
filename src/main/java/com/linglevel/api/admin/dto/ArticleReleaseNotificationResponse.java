package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "아티클 출시 알림 전송 응답")
public class ArticleReleaseNotificationResponse {

    @Schema(description = "총 성공적으로 전송된 알림 수", example = "1250")
    private int totalSentCount;

    @Schema(description = "아티클별 전송 결과")
    private List<ArticleResult> results;

    @Data
    @Builder
    @AllArgsConstructor
    @Schema(description = "아티클별 전송 결과")
    public static class ArticleResult {

        @Schema(description = "아티클 ID", example = "article-123")
        private String articleId;

        @Schema(description = "해당 아티클로 전송된 알림 수", example = "850")
        private int sentCount;

        @Schema(description = "타겟 사용자 수", example = "900")
        private int targetUserCount;
    }
}
