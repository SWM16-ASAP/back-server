package com.linglevel.api.bookmarks.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookmarksErrorCode {
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "Word not found."),
    WORD_ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "Word is already bookmarked."),
    WORD_BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "Word bookmark not found."),
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "Invalid pagination parameters.");
    
    private final HttpStatus status;
    private final String message;
}