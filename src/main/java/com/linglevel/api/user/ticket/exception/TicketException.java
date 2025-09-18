package com.linglevel.api.user.ticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TicketException extends RuntimeException {
    private final HttpStatus status;

    public TicketException(TicketErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
    }
}