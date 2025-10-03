package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Schema(description = "브로드캐스트 알림 전송 요청")
public class NotificationBroadcastRequest {

    @Schema(description = "국가별 메시지 맵 (최소 US 필수)", example = "{\"KR\": {\"title\": \"새 기능\", \"body\": \"확인하세요\"}, \"US\": {\"title\": \"New Feature\", \"body\": \"Check it out\"}}", required = true)
    @NotEmpty(message = "Messages are required")
    @Valid
    private Map<String, LocalizedMessage> messages;

    @Schema(description = "커스텀 데이터 (딥링크, 액션 등)", example = "{\"deeplink\": \"/announcements/123\", \"action\": \"open_announcement\"}")
    private Map<String, String> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "국가별 메시지")
    public static class LocalizedMessage {
        @Schema(description = "알림 제목", example = "새로운 기능이 출시되었습니다!", required = true)
        @NotBlank(message = "Title is required")
        private String title;

        @Schema(description = "알림 내용", example = "지금 바로 확인해보세요.", required = true)
        @NotBlank(message = "Body is required")
        private String body;
    }
}
