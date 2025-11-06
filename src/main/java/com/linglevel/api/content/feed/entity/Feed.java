package com.linglevel.api.content.feed.entity;

import com.linglevel.api.content.common.ContentCategory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feeds")
public class Feed {

    @Id
    private String id;

    private FeedContentType contentType;

    private String title;

    @Indexed(unique = true)
    private String url;

    private String thumbnailUrl;

    private String author;

    @Indexed
    private ContentCategory category;

    private List<String> tags;

    private String sourceProvider;

    @Indexed
    private Instant publishedAt;

    private Integer displayOrder;

    private Integer viewCount;

    private Double avgReadTimeSeconds;

    private Instant createdAt;
}
