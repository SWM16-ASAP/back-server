package com.linglevel.api.content.book.entity;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.TitleTranslations;
import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 500, nullable = false)
	private String title;

	@JdbcTypeCode(SqlTypes.JSON)
	private TitleTranslations titleTranslations;

	@Column(length = 500)
	private String author;

	@Column(length = 2048)
	private String coverImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private DifficultyLevel difficultyLevel;

	private Integer chapterCount;

	private Integer readingTime;

	@Column(nullable = false)
	private Double averageRating = 0.0;

	@Column(nullable = false)
	private Integer reviewCount = 0;

	@Column(nullable = false)
	private Integer viewCount = 0;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

}
