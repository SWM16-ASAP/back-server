package com.linglevel.api.bookmarks.entity;

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
@CompoundIndex(name = "userId_wordId_unique", def = "{'userId': 1, 'wordId': 1}", unique = true)
public class WordBookmark {
    @Id
    private String id;
    
    private String userId;
    
    private String wordId;
    
    private LocalDateTime bookmarkedAt;
}