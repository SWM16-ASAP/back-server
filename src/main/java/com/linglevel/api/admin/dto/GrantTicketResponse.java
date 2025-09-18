package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "어드민 티켓 지급 응답")
public class GrantTicketResponse {
    
    @Schema(description = "응답 메시지", example = "Tickets granted successfully.")
    private String message;
    
    @Schema(description = "티켓을 지급받은 사용자 ID", example = "60d0fe4f5311236168a109ca")
    private String userId;
    
    @Schema(description = "지급한 티켓 수", example = "5")
    private Integer amount;
    
    @Schema(description = "지급 후 새로운 잔고", example = "8")
    private Integer newBalance;
}