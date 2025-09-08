package com.linglevel.api.fcm.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "FCM 플랫폼 타입")
public enum FcmPlatform {
    @Schema(description = "안드로이드 플랫폼")
    ANDROID("android", "Android", "안드로이드 플랫폼"),
    
    @Schema(description = "iOS 플랫폼")
    IOS("ios", "iOS", "iOS 플랫폼"),
    
    @Schema(description = "웹 플랫폼")
    WEB("web", "Web", "웹 플랫폼");

    private final String code;
    private final String name;
    private final String description;

    FcmPlatform(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}