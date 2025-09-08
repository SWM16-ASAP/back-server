package com.linglevel.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CommonException extends RuntimeException {
    private final HttpStatus status;

    public CommonException(CommonErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }

    public CommonException(CommonErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = errorCode.getStatus();
    }
}