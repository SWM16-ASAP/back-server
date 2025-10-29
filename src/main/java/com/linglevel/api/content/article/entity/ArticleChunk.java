package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articleChunks")
@CompoundIndex(name = "article_difficulty_chunk_idx", def = "{'articleId': 1, 'difficultyLevel': 1, 'chunkNumber': 1}")
public class ArticleChunk {
    @Id
    private String id;

    private String articleId;

    private Integer chunkNumber;

    private DifficultyLevel difficultyLevel;

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