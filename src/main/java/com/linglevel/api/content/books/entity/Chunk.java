package com.linglevel.api.content.books.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chunks")
public class Chunk {
    @Id
    private String id;

    private String chapterId;
    
    private Integer chunkNumber;
    
    private DifficultyLevel difficulty;
    
    private ChunkType type;
    
    private String content;
    
    private String description;
} 