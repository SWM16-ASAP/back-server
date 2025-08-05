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
@Schema(description = "OAuth 로그인 요청")
public class OauthLoginRequest {
    @Schema(description = "Firebase Auth Code", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")  
    private String authCode;
} 