package com.linglevel.api.content.book.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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

    private Integer currentReadChunkNumber;

    private Integer maxReadChapterNumber;

    private Integer maxReadChunkNumber;

    private Boolean isCompleted = false;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
