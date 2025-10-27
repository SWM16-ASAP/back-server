package com.linglevel.api.streak.entity;

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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dailyCompletions")
@CompoundIndex(name = "idx_userId_completionDate", def = "{'userId': 1, 'completionDate': 1}", unique = true)
public class DailyCompletion {
    @Id
    private String id;

    @Indexed
    private String userId;
    private LocalDate completionDate;
    private Integer completionCount = 1;
    private List<CompletedContent> completedContents = new ArrayList<>();

    private Instant createdAt;
}