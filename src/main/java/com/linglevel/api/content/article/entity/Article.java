package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.i18n.LanguageCode;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "category_createdAt_idx", def = "{'category': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "viewCount_idx", def = "{'viewCount': -1}"),
    @CompoundIndex(name = "averageRating_idx", def = "{'averageRating': -1}"),
    @CompoundIndex(name = "createdAt_idx", def = "{'createdAt': -1}"),
    @CompoundIndex(name = "targetLanguageCode_createdAt_idx", def = "{'targetLanguageCode': 1, 'createdAt': -1}")
})
@Document(collection = "articles")
public class Article {
    @Id
    private String id;

    @TextIndexed(weight = 5)
    private String title;

    @TextIndexed(weight = 3)
    private String author;

    private String coverImageUrl;

    private String originUrl;
    
    private DifficultyLevel difficultyLevel;
    
    private Integer readingTime;
    
    private Double averageRating;
    
    private Integer reviewCount;
    
    private Integer viewCount;

    private ContentCategory category;

    @Indexed(name = "tags_idx")
    private List<String> tags;

    @Indexed(name = "targetLanguageCode_idx")
    private List<LanguageCode> targetLanguageCode;

    private Instant createdAt;
}
