package com.linglevel.api.words.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WordsException extends RuntimeException {
    private final HttpStatus status;

    public WordsException(WordsErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}