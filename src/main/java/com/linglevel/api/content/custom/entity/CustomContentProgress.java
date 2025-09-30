package com.linglevel.api.content.custom.entity;

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
@Document(collection = "customProgress")
@CompoundIndex(name = "idx_user_custom_progress", def = "{'userId': 1, 'customId': 1}", unique = true)
public class CustomContentProgress {
    @Id
    private String id;

    private String userId;

    private String customId;

    private String chunkId;

    private Integer currentReadChunkNumber;

    private Integer maxReadChunkNumber;

    private Boolean isCompleted = false;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}