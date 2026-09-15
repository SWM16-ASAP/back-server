package com.linglevel.api.content.book.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "book_progress")
public class BookProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long bookId;

	private Long chapterId;

	private String chunkId;

	private Integer currentReadChapterNumber;

	private Integer maxReadChapterNumber;

	/**
	 * 챕터 우선 정렬 기준의 최대 도달 청크 위치값. 비교 순서는 (chapterNumber, chunkNumber)이며 chapter가 우선한다.
	 */
	private Integer maxReadChunkNumber;

	// V2 Progress Fields
	private Double normalizedProgress;

	private Double maxNormalizedProgress;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private DifficultyLevel currentDifficultyLevel;

	/**
	 * 챕터별 진행 상태는 book_chapter_progress의 개별 행으로 저장한다.
	 */
	@OneToMany(mappedBy = "bookProgress", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChapterProgressInfo> chapterProgresses = new ArrayList<>();

	/**
	 * 책 전체 완료 여부 모든 챕터가 완료되었을 때만 true로 설정되는 특수 조건
	 */
	@Column(nullable = false)
	private Boolean isCompleted = false;

	private Instant completedAt;

	@UpdateTimestamp
	private Instant updatedAt = Instant.now();

	@Version
	private Long version;

	/**
	 * 챕터 진행률 정보를 담는 내부 클래스
	 */
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Entity(name = "BookChapterProgress")
	@Table(name = "book_chapter_progress")
	public static class ChapterProgressInfo {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "book_progress_id", nullable = false)
		private BookProgress bookProgress;

		/**
		 * 챕터 번호
		 */
		@Column(nullable = false)
		private Integer chapterNumber;

		/**
		 * 챕터 내 진행률 (0-100%)
		 */
		private Double progressPercentage;

		/**
		 * 챕터 완료 여부
		 */
		@Builder.Default
		@Column(nullable = false)
		private Boolean isCompleted = false;

		/**
		 * 챕터 완료 시점 (첫 완료 시점)
		 */
		private Instant completedAt;

	}

}
