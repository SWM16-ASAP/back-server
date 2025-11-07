package com.linglevel.api.content.feed.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FeedException extends RuntimeException {

    private final HttpStatus status;

    public FeedException(FeedErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}
