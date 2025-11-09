package com.linglevel.api.content.recommendation.entity;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contentAccessLogs")
@CompoundIndexes({
        @CompoundIndex(name = "user_accessed_idx", def = "{'userId': 1, 'accessedAt': -1}"),
        @CompoundIndex(name = "user_category_idx", def = "{'userId': 1, 'category': 1}"),
        @CompoundIndex(name = "user_content_type_idx", def = "{'userId': 1, 'contentType': 1}")
})
public class ContentAccessLog {

    @Id
    private String id;

    private String userId;

    private String contentId;

    private ContentType contentType;

    private ContentCategory category;

    private Integer readTimeSeconds;

    private Instant accessedAt;
}
