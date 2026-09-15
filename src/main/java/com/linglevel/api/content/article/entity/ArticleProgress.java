package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article_progress")
public class ArticleProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long articleId;

	private String chunkId;

	// V2 Progress Fields
	private Double normalizedProgress;

	private Double maxNormalizedProgress;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private DifficultyLevel currentDifficultyLevel;

	@Column(nullable = false)
	private Boolean isCompleted = false;

	private Instant completedAt;

	@UpdateTimestamp
	private Instant updatedAt = Instant.now();

	@Version
	private Long version;

}
