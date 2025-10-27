package com.linglevel.api.streak.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StreakErrorCode {
    STREAK_NOT_FOUND(HttpStatus.NOT_FOUND, "Streak record not found."),
    INVALID_COMPLETION_DATE(HttpStatus.BAD_REQUEST, "Cannot complete content in the future."),
    CONTENT_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "Content information is required."),
    INVALID_READING_TIME(HttpStatus.BAD_REQUEST, "Reading time must be at least 30 seconds.");

    private final HttpStatus status;
    private final String message;
}
