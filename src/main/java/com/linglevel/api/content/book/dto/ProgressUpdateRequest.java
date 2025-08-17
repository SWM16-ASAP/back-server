package com.linglevel.api.content.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "읽기 진도 업데이트 요청")
public class ProgressUpdateRequest {
    @Schema(description = "챕터 ID", example = "60d0fe4f5311236168a109cb")
    private String chapterId;
    
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168c172db")
    private String chunkId;
} 