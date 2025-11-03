package com.linglevel.api.crawling.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crawlingDsl")
public class CrawlingDsl {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String domain;

    private String name;

    private String titleDsl;
    
    private String contentDsl;

    private String thumbnailDsl;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}