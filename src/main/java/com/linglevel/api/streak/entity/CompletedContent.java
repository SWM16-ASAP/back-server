package com.linglevel.api.streak.entity;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletedContent {

    private ContentType type;
    private String contentId;
    private String chapterId;
    private Instant completedAt;
    private Integer readingTime;
    private ContentCategory category;
    private DifficultyLevel difficultyLevel;
}
