package com.linglevel.api.books.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chunks")
public class Chunk {
    @Id
    private String id;
    
    private String chapterId;
    
    private Integer chunkNumber;
    
    private String difficulty;
    
    private String content;
    
    private Boolean isImage;
    
    private String chunkImageUrl;
    
    private String description;
} 