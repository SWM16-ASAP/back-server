package com.linglevel.api.crawling.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CrawlingErrorCode {
    DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "Domain not found."),
    DOMAIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "Domain already exists."),
    INVALID_URL_FORMAT(HttpStatus.BAD_REQUEST, "Invalid URL format."),
    URL_PARAMETER_REQUIRED(HttpStatus.BAD_REQUEST, "URL parameter is required."),
    DOMAIN_AND_DSL_REQUIRED(HttpStatus.BAD_REQUEST, "Domain and dsl are required."),
    DSL_REQUIRED(HttpStatus.BAD_REQUEST, "DSL is required."),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "Invalid API key.");

    private final HttpStatus status;
    private final String message;
}