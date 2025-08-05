package com.linglevel.api.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode {
    INVALID_FIREBASE_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Firebase token."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid access token."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access token has expired."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token has expired.");
    
    private final HttpStatus status;
    private final String message;
} 