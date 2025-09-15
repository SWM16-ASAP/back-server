package com.linglevel.api.content.book.dto;

import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
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

    @Schema(description = "난이도", example = "A1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "청크 타입", example = "TEXT")
    private ChunkType type;
    
    @Schema(description = "내용 (텍스트일 경우 텍스트, 이미지일 경우 URL)", example = "Once when I was six years old...")
    private String content;
    
    @Schema(description = "이미지 설명 (이미지 타입일 경우)", example = "null")
    private String description;
    
    public static ChunkResponse from(Chunk chunk) {
        return ChunkResponse.builder()
                .id(chunk.getId())
                .chunkNumber(chunk.getChunkNumber())
                .difficultyLevel(chunk.getDifficulty())
                .type(chunk.getType())
                .content(chunk.getContent())
                .description(chunk.getDescription())
                .build();
    }
} 