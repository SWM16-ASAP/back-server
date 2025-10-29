package com.linglevel.api.content.custom.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customContentChunks")
@CompoundIndexes({
    @CompoundIndex(name = "custom_content_difficulty_chapter_chunk_idx", def = "{'customContentId': 1, 'difficultyLevel': 1, 'chapterNum': 1, 'chunkNum': 1}"),
    @CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'isDeleted': 1, 'createdAt': -1}")
})
public class CustomContentChunk {
    
    @Id
    private String id;

    @NotNull
    @Indexed
    private String customContentId;

    @NotNull
    @Indexed
    private String userId;

    @NotNull
    private DifficultyLevel difficultyLevel;

    @NotNull
    private Integer chapterNum;

    @NotNull
    private Integer chunkNum;

    @NotNull
    private ChunkType type;

    @NotNull
    private String chunkText;

    private String description;

    @Builder.Default
    private Boolean isDeleted = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}