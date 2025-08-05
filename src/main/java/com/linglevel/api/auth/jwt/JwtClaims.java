package com.linglevel.api.auth.jwt;

import com.linglevel.api.users.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    
    @Schema(description = "사용자 고유 식별자", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "사용자명 (제공자_UID 형식)", example = "google_123456789")
    private String username;
    
    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;
    
    @Schema(description = "사용자 역할", example = "USER")
    private UserRole role;
    
    @Schema(description = "OAuth 제공자", example = "google")
    private String provider;
    
    @Schema(description = "사용자 표시 이름", example = "홍길동")
    private String displayName;
    
    @Schema(description = "토큰 발급 시간", example = "2025-08-04T09:30:00")
    private Date issuedAt;
    
    @Schema(description = "토큰 만료 시간", example = "2025-08-04T19:30:00")
    private Date expiresAt;

    public boolean isExpired() {
        return expiresAt.before(new Date());
    }
}