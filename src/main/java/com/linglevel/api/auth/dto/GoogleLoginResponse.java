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
@Schema(description = "구글 로그인 응답")
public class GoogleLoginResponse {
    @Schema(description = "Access Token")
    private String accessToken;
    
    @Schema(description = "Refresh Token")
    private String refreshToken;
    
    @Schema(description = "사용자 정보")
    private UserResponse user;
} 