package com.linglevel.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그아웃 응답")
public class LogoutResponse {
    @Schema(description = "결과 메시지", example = "Successfully logged out.")
    private String message;
} 