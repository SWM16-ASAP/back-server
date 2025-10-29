package com.linglevel.api.streak.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StreakErrorCode {
    STREAK_NOT_FOUND(HttpStatus.NOT_FOUND, "스트릭 기록을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}