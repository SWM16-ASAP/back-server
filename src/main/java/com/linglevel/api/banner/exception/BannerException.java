package com.linglevel.api.banner.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BannerException extends RuntimeException {

    private final HttpStatus status;

    public BannerException(BannerErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }

    public BannerException(BannerErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = errorCode.getStatus();
    }
}