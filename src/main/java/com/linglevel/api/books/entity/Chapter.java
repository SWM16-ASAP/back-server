package com.linglevel.api.books.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chapters")
public class Chapter {
    @Id
    private String id;
    
    private String bookId;
    
    private Integer chapterNumber;
    
    private Integer chunkCount;
    
    private String title;
    
    private String chapterImageUrl;
    
    private String description;
    
    private Integer readingTime;
} 