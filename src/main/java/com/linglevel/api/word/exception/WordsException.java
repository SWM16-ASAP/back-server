package com.linglevel.api.word.exception;

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