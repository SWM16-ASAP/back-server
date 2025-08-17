package com.linglevel.api.bookmark.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wordBookmarks")
@CompoundIndex(name = "userId_word_unique", def = "{'userId': 1, 'word': 1}", unique = true)
public class WordBookmark {
    @Id
    private String id;
    
    private String userId;
    
    private String word;
    
    private LocalDateTime bookmarkedAt;
}