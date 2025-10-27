package com.linglevel.api.streak.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StreakException extends RuntimeException {
    private final HttpStatus status;

    public StreakException(StreakErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}
