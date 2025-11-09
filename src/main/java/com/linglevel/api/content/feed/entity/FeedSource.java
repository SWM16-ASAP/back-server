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
@Document(collection = "feedSources")
public class FeedSource {

    @Id
    private String id;

    @Indexed(unique = true)
    private String url;

    private String domain;

    private String name;

    private String coverImageDsl;

    private FeedContentType contentType;

    @Indexed
    private ContentCategory category;

    private List<String> tags;

    @Indexed
    private Boolean isActive;

    /**
     * 적용할 필터 목록
     * 예: ["YouTubeShortsFilter", "LanguageFilter", "ContentCrawlabilityFilter"]
     * null이면 모든 필터 적용 안함
     */
    private List<String> enabledFilters;

    private Instant createdAt;

    private Instant updatedAt;
}
