package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "알림 전송 응답")
public class NotificationSendResponse {
    
    @Schema(description = "응답 메시지", example = "Notification sent successfully.")
    private String message;
    
    @Schema(description = "성공적으로 전송된 디바이스 수", example = "2")
    private int sentCount;
    
    @Schema(description = "전송 실패한 디바이스 수", example = "0")
    private int failedCount;
    
    @Schema(description = "전송 결과 상세 정보")
    private NotificationSendDetails details;
    
    @Data
    @AllArgsConstructor
    @Schema(description = "전송 결과 상세")
    public static class NotificationSendDetails {
        
        @Schema(description = "전송 성공한 토큰들", example = "[\"token1\", \"token2\"]")
        private List<String> sentTokens;
        
        @Schema(description = "전송 실패한 토큰들", example = "[]")
        private List<String> failedTokens;
    }
}