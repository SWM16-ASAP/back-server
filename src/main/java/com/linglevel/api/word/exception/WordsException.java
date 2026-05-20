package com.linglevel.api.word.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WordsException extends RuntimeException {
    private final HttpStatus status;
    private final WordsErrorCode errorCode;

    public WordsException(WordsErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }
}
