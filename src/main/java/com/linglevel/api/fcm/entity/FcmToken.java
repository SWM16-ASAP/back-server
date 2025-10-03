package com.linglevel.api.fcm.entity;

import com.linglevel.api.i18n.CountryCode;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fcmTokens")
public class FcmToken {
    
    @Id
    private String id;
    
    @NotNull
    @Indexed
    private String userId;
    
    @NotNull
    private String deviceId;
    
    @NotNull
    private String fcmToken;
    
    @NotNull
    private FcmPlatform platform;

    private CountryCode countryCode;

    private String appVersion;

    private String osVersion;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Indexed(expireAfter = "7776000s") // 90일 자동 삭제
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private Boolean isActive = true;
}