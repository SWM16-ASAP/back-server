package com.linglevel.api.word.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WordsErrorCode {
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "Word not found."),
    WORD_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Word already exists."),
    WORD_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "Word not found with id.");
    
    private final HttpStatus status;
    private final String message;
}