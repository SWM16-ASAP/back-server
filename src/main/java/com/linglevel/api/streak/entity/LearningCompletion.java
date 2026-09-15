package com.linglevel.api.streak.entity;

import com.linglevel.api.content.common.ContentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "learning_completions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "daily_completion_id", nullable = false)
	private DailyCompletion dailyCompletion;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", length = 20, nullable = false)
	private ContentType type;

	@Column(nullable = false)
	private Long contentId;

	private Long chapterId;

	@Builder.Default
	@Column(nullable = false)
	private Instant completedAt = Instant.now();

	private Integer readingTime;

	private String category;

	private String difficultyLevel;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private StreakStatus streakStatus;

}
