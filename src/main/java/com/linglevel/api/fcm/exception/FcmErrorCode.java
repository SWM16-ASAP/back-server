package com.linglevel.api.fcm.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FcmErrorCode {
    FCM_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "fcmToken, deviceId, and platform are required."),
    INVALID_FCM_TOKEN(HttpStatus.BAD_REQUEST, "Invalid FCM token format."),
    INVALID_DEVICE_ID(HttpStatus.BAD_REQUEST, "Invalid device ID format."),
    INVALID_PLATFORM(HttpStatus.BAD_REQUEST, "Invalid platform. Must be one of: ANDROID, IOS, WEB."),
    
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create FCM token."),
    TOKEN_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update FCM token."),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FCM token not found."),
    INVALID_FCM_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "FCM token validation failed."),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send FCM message."),
    
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "Invalid or expired token."),
    
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "not implemented yet.");
    
    private final HttpStatus status;
    private final String message;
}