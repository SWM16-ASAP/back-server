package com.linglevel.api.bookmarks.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BookmarksException extends RuntimeException {
    private final HttpStatus status;

    public BookmarksException(BookmarksErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}