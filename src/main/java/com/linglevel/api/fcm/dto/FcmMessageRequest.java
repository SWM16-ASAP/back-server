package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "FCM 메시지 요청")
public class FcmMessageRequest {

    @Schema(description = "알림 제목", example = "이벤트 안내", required = true)
    private String title;

    @Schema(description = "알림 내용", example = "새로운 이벤트가 시작되었습니다", required = true)
    private String body;

    @Schema(description = "링크/액션 데이터", example = "{\"link\": \"/events/123\", \"type\": \"event\"}")
    private Map<String, String> data;
}