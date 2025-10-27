package com.linglevel.api.streak.entity;

import com.linglevel.api.content.common.ContentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "dailyCompletions")
@CompoundIndex(name = "user_completion_date", def = "{'userId': 1, 'completionDate': 1}", unique = true)
@Getter
@Setter
@Builder
public class DailyCompletion {
    @Id
    private String id;

    @Indexed
    private String userId;

    private LocalDate completionDate;

    @Builder.Default
    private Integer completionCount = 1;

    private List<CompletedContent> completedContents;

    private Instant createdAt;

    @Getter
    @Setter
    @Builder
    public static class CompletedContent {
        private ContentType type;
        private String contentId;
        private String chapterId;
        private Instant completedAt;
        private Integer readingTime;
        private String category;
        private String difficultyLevel;
    }
}
