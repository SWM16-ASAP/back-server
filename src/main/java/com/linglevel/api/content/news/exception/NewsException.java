package com.linglevel.api.content.news.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NewsException extends RuntimeException {
    
    private final HttpStatus status;
    
    public NewsException(NewsErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
    
    public NewsException(NewsErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = errorCode.getStatus();
    }
}