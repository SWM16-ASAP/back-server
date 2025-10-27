package com.linglevel.api.streak.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "userStudyReports")
@Getter
@Setter
public class UserStudyReport {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private Integer currentStreak = 0;

    private Integer longestStreak = 0;

    private LocalDate lastCompletionDate;

    private LocalDate streakStartDate;

    private Integer availableFreezes = 0;

    private Long totalReadingTimeSeconds = 0L;

    private Set<String> completedContentIds = new HashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
}
