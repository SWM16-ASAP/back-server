package com.linglevel.api.fcm.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pushLogs")
public class PushLog {

    @Id
    private String id;

    @Indexed
    private String campaignId;

    @Indexed
    private String userId;

    @Indexed
    private LocalDateTime sentAt;
    private Boolean sentSuccess;
    private LocalDateTime openedAt;

    @CreatedDate
    @Indexed(expireAfter = "15552000s")
    private LocalDateTime createdAt;
}
