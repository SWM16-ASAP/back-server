package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "FCM 메시지 요청")
public class FcmMessageRequest {

    @Schema(description = "알림 제목", example = "새로운 메시지")
    private String title;

    @Schema(description = "알림 내용", example = "안녕하세요! 새로운 알림이 도착했습니다.")
    private String body;

    @Schema(description = "알림 이미지 URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "추가 데이터", example = "{\"type\": \"news\", \"id\": \"123\"}")
    private Map<String, String> data;
}