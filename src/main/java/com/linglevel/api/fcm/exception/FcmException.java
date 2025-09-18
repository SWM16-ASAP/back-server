package com.linglevel.api.fcm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FcmException extends RuntimeException {
    private final HttpStatus status;

    public FcmException(FcmErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}