package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "브로드캐스트 알림 전송 응답")
public class NotificationBroadcastResponse {

    @Schema(description = "응답 메시지", example = "Broadcast notification sent successfully.")
    private String message;

    @Schema(description = "알림을 전송한 총 사용자 수", example = "1500")
    private int totalUsers;

    @Schema(description = "성공적으로 전송된 디바이스(토큰) 수", example = "2850")
    private int sentCount;

    @Schema(description = "전송 실패한 디바이스(토큰) 수", example = "150")
    private int failedCount;

    @Schema(description = "전송 결과 상세 정보")
    private NotificationBroadcastDetails details;

    @Data
    @AllArgsConstructor
    @Schema(description = "브로드캐스트 전송 결과 상세")
    public static class NotificationBroadcastDetails {

        @Schema(description = "최소 1개 이상의 디바이스에 성공적으로 전송된 사용자 수", example = "1450")
        private int successfulUsers;

        @Schema(description = "모든 디바이스에 전송 실패한 사용자 수", example = "50")
        private int failedUsers;

        @Schema(description = "전송 시도한 총 토큰 수", example = "3000")
        private int totalTokens;
    }
}
