package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "알림 전송 요청")
public class NotificationSendRequest {

    @Schema(description = "알림을 받을 사용자 ID 목록", example = "[\"userId1\", \"userId2\"]", required = true)
    @NotEmpty(message = "Targets are required")
    private List<String> targets;

    @Schema(description = "국가별 메시지 맵 (최소 US 필수)", example = "{\"KR\": {\"title\": \"이벤트\", \"body\": \"시작\"}, \"US\": {\"title\": \"Event\", \"body\": \"Started\"}}", required = true)
    @NotEmpty(message = "Messages are required")
    @Valid
    private Map<String, LocalizedMessage> messages;

    @Schema(description = "커스텀 데이터 (딥링크, 액션 등)", example = "{\"deeplink\": \"/books/60d0fe4f5311236168a109ca\", \"action\": \"open_book\"}")
    private Map<String, String> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "국가별 메시지")
    public static class LocalizedMessage {
        @Schema(description = "알림 제목", example = "이벤트 안내", required = true)
        @NotBlank(message = "Title is required")
        private String title;

        @Schema(description = "알림 내용", example = "새로운 이벤트가 시작되었습니다", required = true)
        @NotBlank(message = "Body is required")
        private String body;
    }
}