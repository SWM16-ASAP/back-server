package com.linglevel.api.content.custom.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomContentException extends RuntimeException {
    private final HttpStatus status;

    public CustomContentException(CustomContentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }

    public CustomContentException(CustomContentErrorCode errorCode, String additionalMessage) {
        super(errorCode.getMessage() + " " + additionalMessage);
        this.status = errorCode.getStatus();
    }
}