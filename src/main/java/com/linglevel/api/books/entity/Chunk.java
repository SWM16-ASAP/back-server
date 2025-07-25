package com.linglevel.api.books.entity;

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
    
    private String content;
    
    private Boolean isImage;
    
    private String chunkImageUrl;
    
    private String description;
} 