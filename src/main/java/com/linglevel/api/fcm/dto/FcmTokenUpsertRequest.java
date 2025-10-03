package com.linglevel.api.fcm.dto;

import com.linglevel.api.fcm.entity.FcmPlatform;
import com.linglevel.api.i18n.CountryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "FCM 토큰 등록/업데이트 요청")
public class FcmTokenUpsertRequest {

    @NotBlank(message = "FCM token is required")
    @Schema(description = "Firebase Cloud Messaging 토큰", example = "fK7JxVxGTkOZ9h_0XYzGjI:APA91bHxyz...")
    private String fcmToken;

    @NotBlank(message = "Device ID is required")
    @Schema(description = "디바이스 고유 식별자", example = "device-uuid-123")
    private String deviceId;

    @NotNull(message = "Platform is required")
    @Schema(description = "플랫폼 종류", example = "ANDROID", allowableValues = {"ANDROID", "IOS", "WEB"})
    private FcmPlatform platform;

    @Schema(description = "디바이스 국가 코드 (기본값: US)", example = "KR", defaultValue = "US")
    private CountryCode countryCode;

    @Schema(description = "앱 버전", example = "1.2.3")
    private String appVersion;

    @Schema(description = "OS 버전", example = "iOS 17.1")
    private String osVersion;
}