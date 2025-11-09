package com.linglevel.api.content.feed.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode {
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "Feed not found."),
    INVALID_SORT_ORDER(HttpStatus.BAD_REQUEST, "Invalid sort_order parameter. Must be one of: RECOMMENDED, LATEST, POPULAR."),
    INVALID_CONTENT_TYPES(HttpStatus.BAD_REQUEST, "Invalid content_types parameter.");

    private final HttpStatus status;
    private final String message;
}
