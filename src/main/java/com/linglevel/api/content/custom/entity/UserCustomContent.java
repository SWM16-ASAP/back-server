package com.linglevel.api.content.custom.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 유저와 커스텀 콘텐츠 간의 매핑 엔티티 한 콘텐츠를 여러 유저가 공유할 수 있도록 N:M 관계 구현
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_custom_contents",
		uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "custom_content_id" }))
public class UserCustomContent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "custom_content_id", nullable = false)
	private Long customContentId;

	@Column(name = "content_request_id", nullable = false)
	private Long contentRequestId;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant unlockedAt;

}
