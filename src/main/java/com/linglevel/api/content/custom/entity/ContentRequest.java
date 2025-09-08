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
@Document(collection = "contentRequests")
public class ContentRequest {
    
    @Id
    private String id;

    @NotNull
    @Indexed
    private String userId;

    @NotNull
    private String title;

    @NotNull
    private ContentType contentType;

    private String originAuthor;

    private List<DifficultyLevel> targetDifficultyLevels;

    private String originUrl;

    private String originDomain;

    @NotNull
    @Builder.Default
    private ContentRequestStatus status = ContentRequestStatus.PENDING;

    @Builder.Default
    private Integer progress = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private LocalDateTime deletedAt;

    private String errorMessage;

    private String resultCustomContentId;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}