package com.linglevel.api.streak.entity;

import lombok.Getter;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "user_study_reports")
public class UserStudyReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	private Integer currentStreak = 0;

	private Integer longestStreak = 0;

	private LocalDate lastCompletionDate;

	private LocalDate streakStartDate;

	private Instant lastLearningTimestamp;

	private Integer availableFreezes = 0;

	private Long totalReadingTimeSeconds = 0L;

	private Integer preferredStudyHour;

	private Instant preferredStudyHourUpdatedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@org.hibernate.annotations.UpdateTimestamp
	private Instant updatedAt;

	@Version
	private Long version;

}
