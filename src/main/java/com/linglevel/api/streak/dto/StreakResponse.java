package com.linglevel.api.streak.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreakResponse {
    private int currentStreak;
    private int longestStreak;
    private long totalReadingTimeSeconds;
    private String encouragementMessage;
    private double percentile;
    private boolean isCompletedToday;
    private int availableFreezes;
}