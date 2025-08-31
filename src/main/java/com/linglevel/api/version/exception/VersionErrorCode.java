package com.linglevel.api.version.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum VersionErrorCode {
    VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Version information not found."),
    VERSION_FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "At least one version field (latestVersion or minimumVersion) must be provided.");
    
    private final HttpStatus status;
    private final String message;
}