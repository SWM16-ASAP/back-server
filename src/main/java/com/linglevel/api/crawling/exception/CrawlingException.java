package com.linglevel.api.crawling.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CrawlingException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public CrawlingException(CrawlingErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
        this.errorCode = errorCode.name();
    }

    public CrawlingException(CrawlingErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = errorCode.getStatus();
        this.errorCode = errorCode.name();
    }
}