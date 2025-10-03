package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
@Schema(description = "브로드캐스트 알림 전송 요청")
public class NotificationBroadcastRequest {

    @Schema(description = "알림 제목", example = "새로운 기능이 출시되었습니다!", required = true)
    @NotBlank(message = "Title is required")
    private String title;

    @Schema(description = "알림 내용", example = "지금 바로 확인해보세요.", required = true)
    @NotBlank(message = "Body is required")
    private String body;

    @Schema(description = "커스텀 데이터 (딥링크, 액션 등)", example = "{\"deeplink\": \"/announcements/123\", \"action\": \"open_announcement\"}")
    private Map<String, String> data;
}
