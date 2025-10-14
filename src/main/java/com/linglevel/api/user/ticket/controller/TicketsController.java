package com.linglevel.api.user.ticket.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.user.ticket.dto.GetTicketTransactionsRequest;
import com.linglevel.api.user.ticket.dto.TicketBalanceResponse;
import com.linglevel.api.user.ticket.dto.TicketTransactionResponse;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tickets", description = "티켓 관련 API")
public class TicketsController {

    private final TicketService ticketService;

    @GetMapping("/balance")
    @Operation(summary = "티켓 잔고 조회", description = "사용자의 현재 티켓 잔고를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "잔고 조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public ResponseEntity<TicketBalanceResponse> getTicketBalance(@AuthenticationPrincipal JwtClaims claims) {
        TicketBalanceResponse response = ticketService.getTicketBalance(claims.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "티켓 거래 내역 조회", description = "사용자의 티켓 거래 내역을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "거래 내역 조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    public ResponseEntity<PageResponse<TicketTransactionResponse>> getTicketTransactions(
            @ParameterObject @Valid @ModelAttribute GetTicketTransactionsRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        var transactions = ticketService.getTicketTransactions(
            claims.getId(),
            request.getPage(),
            request.getLimit()
        );

        PageResponse<TicketTransactionResponse> response = new PageResponse<>(
            transactions.getContent(),
            transactions
        );

        return ResponseEntity.ok(response);
    }


    @ExceptionHandler(TicketException.class)
    public ResponseEntity<ExceptionResponse> handleTicketException(TicketException e) {
        log.error("Ticket Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}