package com.linglevel.api.content.recommendation.entity;

import com.linglevel.api.content.common.ContentCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_category_preferences")
public class UserCategoryPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ContentCategory primaryCategory;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<ContentCategory, Double> categoryScores;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<ContentCategory, Integer> rawAccessCounts;

	private Integer totalAccessCount;

	private Instant lastUpdatedAt;

}
