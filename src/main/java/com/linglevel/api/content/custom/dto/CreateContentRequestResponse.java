package com.linglevel.api.content.custom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "콘텐츠 처리 요청 생성 응답")
public class CreateContentRequestResponse {
    
    @Schema(description = "생성된 요청 ID", example = "60d0fe4f5311236168a109ca")
    private String id;
    
    @Schema(description = "요청 상태", example = "PENDING")
    private String status;
    
    @Schema(description = "응답 메시지", example = "Content processing request created successfully")
    private String message;
}