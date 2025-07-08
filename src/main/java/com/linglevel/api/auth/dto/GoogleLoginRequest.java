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
@Schema(description = "구글 로그인 요청")
public class GoogleLoginRequest {
    @Schema(description = "구글 Authorization Code", example = "4/0AX4XfWh...")
    private String authCode;
} 