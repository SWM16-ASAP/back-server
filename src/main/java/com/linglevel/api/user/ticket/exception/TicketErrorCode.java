package com.linglevel.api.user.ticket.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TicketErrorCode {
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "Insufficient ticket balance."),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "Ticket not found."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Invalid ticket amount."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Ticket reservation not found.");

    private final HttpStatus status;
    private final String message;
}