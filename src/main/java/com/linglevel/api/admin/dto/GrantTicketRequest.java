package com.linglevel.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "어드민 티켓 지급 요청")
public class GrantTicketRequest {
    
    @Schema(description = "티켓을 지급받을 사용자 ID", example = "60d0fe4f5311236168a109ca", required = true)
    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;
    
    @Schema(description = "지급할 티켓 수", example = "5", required = true)
    @NotNull(message = "지급할 티켓 수는 필수입니다.")
    @Min(value = 1, message = "지급할 티켓 수는 1 이상이어야 합니다.")
    private Integer amount;
    
    @Schema(description = "지급 사유", example = "구독 갱신 보상")
    private String reason;
}