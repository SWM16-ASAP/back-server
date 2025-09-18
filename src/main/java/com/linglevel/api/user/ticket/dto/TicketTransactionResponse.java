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
@Schema(description = "티켓 거래 내역 응답")
public class TicketTransactionResponse {
    
    @Schema(description = "거래 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "티켓 변화량 (양수: 획득, 음수: 사용)", example = "-1")
    private Integer amount;
    
    @Schema(description = "거래 설명", example = "콘텐츠 생성 (My Custom Article)")
    private String description;
    
    @Schema(description = "거래 생성 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
}