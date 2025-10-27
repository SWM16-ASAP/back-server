package com.linglevel.api.content.book.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookProgress")
@CompoundIndex(name = "idx_user_book_progress", def = "{'userId': 1, 'bookId': 1}", unique = true)
public class BookProgress {
    @Id
    private String id;

    private String userId;

    private String bookId;

    private String chapterId;

    private String chunkId;

    private Integer currentReadChapterNumber;

    private Integer maxReadChapterNumber;

    // V2 Progress Fields
    private Double normalizedProgress;

    private Double maxNormalizedProgress;

    private DifficultyLevel currentDifficultyLevel;

    private Boolean isCompleted = false;

    private Instant completedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
