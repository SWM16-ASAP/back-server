package com.linglevel.api.fcm.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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

    @Indexed(unique = true)
    private String campaignId;  // 각 메시지의 고유 ID (자체 UUID)

    private String fcmMessageId;  // FCM messageId (선택적, FCM 추적용)

    @Indexed
    private String campaignGroup;  // 내부 그룹화용 (선택적)

    @Indexed
    private String userId;

    @Indexed
    private LocalDateTime sentAt;
    private Boolean sentSuccess;
    private LocalDateTime openedAt;

    @CreatedDate
    @Indexed(expireAfter = "15552000s")
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
