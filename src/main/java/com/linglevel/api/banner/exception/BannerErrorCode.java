package com.linglevel.api.banner.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BannerErrorCode {
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "Banner not found."),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Content not found."),
    INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "Unsupported content type. Must be BOOK or ARTICLE."),
    COUNTRY_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "CountryCode is required."),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "Invalid API key."),
    UPDATE_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "At least one field must be provided for update.");

    private final HttpStatus status;
    private final String message;
}