package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Schema(description = "FCM 메시지 요청")
public class FcmMessageRequest {

    @Schema(description = "알림 제목", example = "Content Ready", required = true)
    private String title;

    @Schema(description = "알림 내용", example = "Your content has been successfully processed", required = true)
    private String body;

    @Schema(description = "알림 타입", example = "custom_content_completed")
    private String type;

    @Schema(description = "사용자 ID", example = "user123")
    private String userId;

    @Schema(description = "탭했을 때 수행할 액션", example = "view_content")
    private String action;

    @Schema(description = "딥링크 URL", example = "/custom-content/123")
    private String deepLink;

    @Schema(description = "추가 데이터", example = "{\"requestId\": \"req123\", \"contentTitle\": \"My Content\"}")
    private Map<String, String> additionalData;

    @Schema(description = "최종 FCM data (자동 생성)", hidden = true)
    private Map<String, String> data;

    public Map<String, String> buildFcmData() {
        Map<String, String> fcmData = new HashMap<>();

        if (type != null) fcmData.put("type", type);
        if (userId != null) fcmData.put("userId", userId);
        if (action != null) fcmData.put("action", action);
        if (deepLink != null) fcmData.put("deepLink", deepLink);

        if (additionalData != null) {
            fcmData.putAll(additionalData);
        }

        return fcmData;
    }

    public Map<String, String> getData() {
        if (data == null) {
            data = buildFcmData();
        }
        return data;
    }
}