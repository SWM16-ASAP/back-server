package com.linglevel.api.word.entity;

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
@Document(collection = "invalidWords")
public class InvalidWord {
    @Id
    private String id;

    @Indexed(unique = true)
    private String word;

    private LocalDateTime attemptedAt;

    @Builder.Default
    private Integer attemptCount = 1;

    @Indexed(name = "ttl_expires_at", expireAfter = "0s")
    private LocalDateTime expiresAt;
}
