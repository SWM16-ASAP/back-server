package com.linglevel.api.users.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UsersException extends RuntimeException {
    private final HttpStatus status;

    public UsersException(UsersErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
} 