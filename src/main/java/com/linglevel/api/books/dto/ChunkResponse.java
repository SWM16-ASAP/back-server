package com.linglevel.api.books.dto;

import com.linglevel.api.books.entity.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "청크 정보 응답")
public class ChunkResponse {
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
    private String id;
    
    @Schema(description = "청크 번호", example = "1")
    private Integer chunkNumber;
    
    @Schema(description = "난이도 레벨", example = "A1")
    private DifficultyLevel difficulty;
    
    @Schema(description = "텍스트 내용", example = "Once when I was six years old...")
    private String content;
    
    @Schema(description = "이미지 청크 여부", example = "false")
    private Boolean isImage;
    
    @Schema(description = "청크 이미지 URL", example = "null")
    private String chunkImageUrl;
    
    @Schema(description = "이미지 설명", example = "null")
    private String description;
} 