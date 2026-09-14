package com.linglevel.api.content.custom.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_requests")
public class ContentRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "request_key", nullable = false, unique = true, length = 36, updatable = false)
	private String requestKey;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "ticket_reservation_id", nullable = false, unique = true)
	private Long ticketReservationId;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String originalText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContentType contentType;

	private String originAuthor;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<DifficultyLevel> targetDifficultyLevels;

	private String originUrl;

	private String originDomain;

	private String coverImageUrl;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContentRequestStatus status = ContentRequestStatus.PENDING;

	@Builder.Default
	private Integer progress = 0;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Instant completedAt;

	private Instant deletedAt;

	private String errorMessage;

	private String resultCustomContentId;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

}
