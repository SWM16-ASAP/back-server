package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "FCM 토큰 업데이트 응답")
public class FcmTokenUpdateResponse {

    @Schema(description = "응답 메시지", example = "FCM token updated successfully.")
    private String message;

    @Schema(description = "업데이트된 토큰 ID", example = "60d0fe4f5311236168a109ca")
    private String tokenId;
}