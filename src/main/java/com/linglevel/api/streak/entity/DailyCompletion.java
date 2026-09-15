package com.linglevel.api.streak.entity;

import com.linglevel.api.content.common.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_completions")
public class DailyCompletion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDate completionDate;

	@Builder.Default
	private Integer firstCompletionCount = 0;

	@Builder.Default
	private Integer totalCompletionCount = 0;

	@Builder.Default
	@OneToMany(mappedBy = "dailyCompletion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LearningCompletion> completedContents = new java.util.ArrayList<>();

	private Integer streakCount;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private StreakStatus streakStatus;

	@Builder.Default
	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

}
