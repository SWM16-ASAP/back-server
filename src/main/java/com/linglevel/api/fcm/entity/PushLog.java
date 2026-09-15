package com.linglevel.api.fcm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "push_logs")
public class PushLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 36)
	private String campaignId; // 각 메시지의 고유 ID (자체 UUID)

	@Column(length = 255)
	private String fcmMessageId; // FCM messageId (선택적, FCM 추적용)

	@Column(length = 255)
	private String campaignGroup; // 내부 그룹화용 (선택적)

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDateTime sentAt;

	@Column(nullable = false)
	private Boolean sentSuccess;

	private LocalDateTime openedAt;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Version
	private Long version;

}
