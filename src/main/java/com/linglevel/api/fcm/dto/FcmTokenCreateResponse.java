package com.linglevel.api.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "FCM 토큰 생성 응답")
public class FcmTokenCreateResponse {

    @Schema(description = "응답 메시지", example = "FCM token created successfully.")
    private String message;

    @Schema(description = "생성된 토큰 ID", example = "60d0fe4f5311236168a109ca")
    private String tokenId;
}