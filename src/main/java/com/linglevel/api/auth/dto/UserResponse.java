package com.linglevel.api.auth.dto;

import com.linglevel.api.users.entity.UserRole;
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
@Schema(description = "사용자 정보 응답")
public class UserResponse {
    @Schema(description = "객체 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "사용자 ID", example = "google_12239")
    private String username;
    
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    
    @Schema(description = "표시 이름", example = "홍길동")
    private String displayName;
    
    @Schema(description = "OAuth 제공업체", example = "google")
    private String provider;
    
    @Schema(description = "프로필 이미지 URL", example = "https://path/to/image.jpg")
    private String profileImageUrl;
    
    @Schema(description = "사용자 역할")
    private UserRole role;
    
    @Schema(description = "계정 생성 시간")
    private LocalDateTime createdAt;
} 