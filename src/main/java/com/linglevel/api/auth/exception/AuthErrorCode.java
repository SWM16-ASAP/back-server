package com.linglevel.api.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode {
    INVALID_GOOGLE_AUTH_CODE(HttpStatus.UNAUTHORIZED, "Invalid Google authorization code."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired.");
    
    private final HttpStatus status;
    private final String message;
} 