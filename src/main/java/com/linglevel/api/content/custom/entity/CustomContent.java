package com.linglevel.api.content.custom.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customContents")
public class CustomContent {
    
    @Id
    private String id;

    @NotNull
    @Indexed
    private String userId;

    @NotNull
    @Indexed
    private String contentRequestId;

    @Builder.Default
    private Boolean isDeleted = false;

    @NotNull
    private String title;

    private String author;

    private String coverImageUrl;

    @NotNull
    private Integer chunkCount;

    @NotNull
    private DifficultyLevel difficultyLevel;

    private List<DifficultyLevel> targetDifficultyLevels;

    private Integer readingTime;

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private Integer viewCount = 0;

    private List<String> tags;

    private String originUrl;

    private String originDomain;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}