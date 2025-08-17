package com.linglevel.api.content.book.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookProgress")
public class BookProgress {
    @Id
    private String id;

    private String userId;

    private String bookId;

    private String chapterId;

    private String chunkId;

    private Integer currentReadChapterNumber;

    private Integer currentReadChunkNumber;
}
