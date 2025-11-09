package com.linglevel.api.content.custom.entity;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 유저와 커스텀 콘텐츠 간의 매핑 엔티티
 * 한 콘텐츠를 여러 유저가 공유할 수 있도록 N:M 관계 구현
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "userCustomContents")
@CompoundIndex(name = "user_content_idx", def = "{'userId': 1, 'customContentId': 1}", unique = true)
public class UserCustomContent {

    @Id
    private String id;

    @NotNull
    @Indexed
    private String userId;

    @NotNull
    @Indexed
    private String customContentId;

    @NotNull
    @Indexed
    private String contentRequestId;

    @CreatedDate
    private Instant unlockedAt;
}