package com.linglevel.api.version.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class VersionException extends RuntimeException {
    private final HttpStatus status;

    public VersionException(VersionErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}