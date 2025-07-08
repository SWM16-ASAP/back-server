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
@Schema(description = "리프레시 토큰 요청")
public class RefreshTokenRequest {
    @Schema(description = "Refresh Token")
    private String refreshToken;
} 