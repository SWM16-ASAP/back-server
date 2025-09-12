package com.linglevel.api.user.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 잔고 응답")
public class TicketBalanceResponse {
    
    @Schema(description = "보유 티켓 수", example = "5")
    private Integer balance;
    
    @Schema(description = "마지막 업데이트 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
}