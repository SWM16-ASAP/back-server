package com.linglevel.api.content.article.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "articleProgress")
@CompoundIndex(name = "idx_user_article_progress", def = "{'userId': 1, 'articleId': 1}", unique = true)
public class ArticleProgress {
    @Id
    private String id;

    private String userId;

    private String articleId;

    private String chunkId;

    private Integer currentReadChunkNumber;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}