package com.linglevel.api.streak.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userStudyReports")
public class UserStudyReport {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;
    private Integer currentStreak = 0;
    private Integer longestStreak = 0;
    private LocalDate lastCompletionDate;
    private LocalDate streakStartDate;
    private Integer freezeCount = 0;
    private Long totalReadingTimeSeconds = 0L;
    private Instant createdAt;
    private Instant updatedAt;
}