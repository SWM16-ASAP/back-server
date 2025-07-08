package com.linglevel.api.books.dto;

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
    
    @Schema(description = "읽은 청크 번호", example = "5")
    private Integer chunkNumber;
} 