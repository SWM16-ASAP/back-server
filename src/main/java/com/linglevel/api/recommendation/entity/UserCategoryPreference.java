package com.linglevel.api.recommendation.entity;

import com.linglevel.api.content.common.ContentCategory;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userCategoryPreferences")
public class UserCategoryPreference {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Indexed
    private ContentCategory primaryCategory;

    private Map<ContentCategory, Double> categoryScores;

    private Map<ContentCategory, Integer> rawAccessCounts;

    private Integer totalAccessCount;

    private LocalDateTime lastUpdatedAt;
}
