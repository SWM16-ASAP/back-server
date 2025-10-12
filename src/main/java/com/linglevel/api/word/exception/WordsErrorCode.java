package com.linglevel.api.word.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WordsErrorCode {
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "Word not found."),
    WORD_IS_MEANINGLESS(HttpStatus.BAD_REQUEST, "The word is meaningless."),
    WORD_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Word already exists."),
    WORD_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "Word not found with id."),
    INVALID_WORD_FORMAT(HttpStatus.BAD_REQUEST, "Word contains invalid characters (spaces, tabs, newlines, or special characters are not allowed)."),
    WORD_TOO_LONG(HttpStatus.BAD_REQUEST, "Word is too long (maximum 50 characters).");

    private final HttpStatus status;
    private final String message;
}