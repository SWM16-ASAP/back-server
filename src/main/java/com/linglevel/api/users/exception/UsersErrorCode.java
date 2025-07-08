package com.linglevel.api.users.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UsersErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "No reading progress found for this user."),
    INVALID_USER_DATA(HttpStatus.BAD_REQUEST, "Invalid user data.");
    
    private final HttpStatus status;
    private final String message;
} 