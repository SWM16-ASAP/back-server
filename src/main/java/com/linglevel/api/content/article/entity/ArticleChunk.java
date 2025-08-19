package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articleChunks")
public class ArticleChunk {
    @Id
    private String id;

    private String articleId;

    private Integer chunkNumber;

    private DifficultyLevel difficulty;

    private ChunkType type;

    private String content;

    private String description;
    
    public void updateContent(String content, String description) {
        this.content = content;
        if (description != null) {
            this.description = description;
        }
    }
}