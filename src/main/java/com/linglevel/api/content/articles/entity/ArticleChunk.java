package com.linglevel.api.content.news.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "newsChunks")
public class NewsChunk {
    @Id
    private String id;

    private String newsId;

    private Integer chunkNumber;

    private DifficultyLevel difficulty;

    private ChunkType type;

    private String content;

    private String description;
}