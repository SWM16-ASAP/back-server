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
@Schema(description = "사용자 정보 응답")
public class UserResponse {
    @Schema(description = "사용자 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    
    @Schema(description = "프로필 이미지 URL", example = "https://path/to/image.jpg")
    private String profileImageUrl;
    
    @Schema(description = "사용자 역할", example = "user")
    private String role;
    
    @Schema(description = "구독 유형", example = "premium")
    private String subscription;
} 