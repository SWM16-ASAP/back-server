package com.linglevel.api.content.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.linglevel.api.content.common.ContentType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentContentResponse {
    private String contentId;
    private ContentType contentType;
    private String title;
    private String author;
    private String coverImageUrl;
    private String difficultyLevel;
    private List<String> tags;
    private Integer readingTime;

    // Progress fields
    private Integer chapterCount;
    private Integer currentReadChapterNumber;
    private Integer chunkCount;
    private Integer currentReadChunkNumber;
    private Double progressPercentage;
    private Boolean isCompleted;

    // CustomContent specific fields
    private String originUrl;
    private String originDomain;

    private Instant lastStudiedAt;
}
