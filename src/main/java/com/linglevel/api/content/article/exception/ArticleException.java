package com.linglevel.api.content.article.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ArticleException extends RuntimeException {
    
    private final HttpStatus status;
    
    public ArticleException(ArticleErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
    
    public ArticleException(ArticleErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.status = errorCode.getStatus();
    }
}