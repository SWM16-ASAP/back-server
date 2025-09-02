package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "커스텀 콘텐츠 청크 응답")
public class CustomContentChunkResponse {
    
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
    private String id;
    
    @Schema(description = "청크 번호", example = "1")
    private Integer chunkNumber;
    
    @Schema(description = "난이도", example = "A1")
    private DifficultyLevel difficulty;
    
    @Schema(description = "타입", example = "TEXT")
    private ChunkType type;
    
    @Schema(description = "레벨링된 텍스트 또는 이미지 URL", 
            example = "Once upon a time, there was a little prince. He lived on a small planet...")
    private String content;
    
    @Schema(description = "청크 설명 (이미지 타입인 경우)", example = "The little prince on his planet")
    private String description;
}