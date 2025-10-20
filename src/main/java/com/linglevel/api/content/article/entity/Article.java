package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.i18n.LanguageCode;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articles")
public class Article {
    @Id
    private String id;

    private String title;

    private String author;

    private String coverImageUrl;

    private String originUrl;
    
    private DifficultyLevel difficultyLevel;
    
    private Integer readingTime;
    
    private Double averageRating;
    
    private Integer reviewCount;
    
    private Integer viewCount;
    
    private List<String> tags;

    private List<LanguageCode> targetLanguageCode;

    private LocalDateTime createdAt;
}