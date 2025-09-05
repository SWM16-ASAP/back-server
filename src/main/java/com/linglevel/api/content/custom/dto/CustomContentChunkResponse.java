package com.linglevel.api.content.custom.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "커스텀 콘텐츠 청크 응답")
public class CustomContentChunkResponse {
    
    @Schema(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
    private String id;
    
    @Schema(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
    private String customContentId;
    
    @Schema(description = "난이도", example = "A1")
    private DifficultyLevel difficultyLevel;
    
    @Schema(description = "챕터 번호", example = "1")
    private Integer chapterNum;
    
    @Schema(description = "청크 번호", example = "1")
    private Integer chunkNum;
    
    @Schema(description = "이미지 여부", example = "false")
    private Boolean isImage;
    
    @Schema(description = "청크 텍스트 내용", 
            example = "Once upon a time, there was a little prince. He lived on a small planet...")
    private String chunkText;
    
    @Schema(description = "청크 설명", example = "Story introduction with simple vocabulary")
    private String description;
    
    @Schema(description = "생성일시", example = "2024-01-15T10:05:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-15T10:05:00")
    private LocalDateTime updatedAt;
}