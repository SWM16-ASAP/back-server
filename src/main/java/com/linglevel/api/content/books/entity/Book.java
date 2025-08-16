package com.linglevel.api.content.books.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
public class Book {
    @Id
    private String id;

    private String title;

    private String author;

    private String coverImageUrl;
    
    private DifficultyLevel difficultyLevel;
    
    private Integer chapterCount;
    
    private Integer readingTime;
    
    private Double averageRating;
    
    private Integer reviewCount;
    
    private Integer viewCount;
    
    private List<String> tags;

    private LocalDateTime createdAt;
}