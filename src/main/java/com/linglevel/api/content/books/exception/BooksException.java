package com.linglevel.api.content.books.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BooksException extends RuntimeException {
    private final HttpStatus status;

    public BooksException(BooksErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
} 