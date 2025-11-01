package com.linglevel.api.streak.entity;

import com.linglevel.api.content.common.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Document(collection = "dailyCompletions")
@CompoundIndex(name = "idx_userId_completionDate", def = "{'userId': 1, 'completionDate': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCompletion {
    @Id
    private String id;

    @Indexed
    private String userId;

    private LocalDate completionDate;

    @Builder.Default
    private Integer firstCompletionCount = 0;

    @Builder.Default
    private Integer totalCompletionCount = 0;

    private List<CompletedContent> completedContents;

    private Integer streakCount;

    private Instant createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
