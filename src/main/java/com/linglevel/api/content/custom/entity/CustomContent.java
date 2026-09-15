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
@Table(name = "custom_contents")
public class CustomContent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "content_request_id", nullable = false, unique = true)
	private Long contentRequestId;

	@Column(name = "creator_user_id", nullable = false)
	private Long userId;

	@Builder.Default
	@Column(name = "deleted", nullable = false)
	private Boolean isDeleted = false;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(length = 500)
	private String author;

	@Column(length = 2048)
	private String coverImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DifficultyLevel difficultyLevel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<DifficultyLevel> targetDifficultyLevels;

	private Integer readingTime;

	@Builder.Default
	private Double averageRating = 0.0;

	@Builder.Default
	private Integer reviewCount = 0;

	@Builder.Default
	private Integer viewCount = 0;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> tags;

	@Column(length = 2048)
	private String originUrl;

	private String originDomain;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	private Instant deletedAt;

}
