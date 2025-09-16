package com.linglevel.api.content.article.dto;

import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기사 청크 응답")
public class ArticleChunkResponse {
    
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
    private String id;
    
    @Schema(description = "청크 번호", example = "1")
    private Integer chunkNumber;

    @Schema(description = "난이도", example = "A1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "청크 타입", example = "TEXT")
    private ChunkType type;
    
    @Schema(description = "내용 (텍스트 또는 이미지 URL)", example = "You have a phone. The phone has a sign...")
    private String content;
    
    @Schema(description = "설명 (이미지인 경우)", example = "Bluetooth logo symbol")
    private String description;
    
    public static ArticleChunkResponse from(ArticleChunk chunk) {
        return ArticleChunkResponse.builder()
                .id(chunk.getId())
                .chunkNumber(chunk.getChunkNumber())
                .difficultyLevel(chunk.getDifficultyLevel())
                .type(chunk.getType())
                .content(chunk.getContent())
                .description(chunk.getDescription())
                .build();
    }
}