package com.linglevel.api.content.book.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 챕터별 진행률 정보 (배열 구조)
     * 각 챕터의 진행 상태, 완료 여부, 완료 시점을 저장
     */
    private List<ChapterProgressInfo> chapterProgresses = new ArrayList<>();

    /**
     * 책 전체 완료 여부
     * 모든 챕터가 완료되었을 때만 true로 설정되는 특수 조건
     */
    private Boolean isCompleted = false;

    private Instant completedAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * 챕터 진행률 정보를 담는 내부 클래스
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterProgressInfo {
        /**
         * 챕터 번호
         */
        private Integer chapterNumber;

        /**
         * 챕터 내 진행률 (0-100%)
         */
        private Double progressPercentage;

        /**
         * 챕터 완료 여부
         */
        private Boolean isCompleted;

        /**
         * 챕터 완료 시점 (첫 완료 시점)
         */
        private Instant completedAt;
    }
}
