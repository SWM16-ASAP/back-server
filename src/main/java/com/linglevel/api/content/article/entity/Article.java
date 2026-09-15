package com.linglevel.api.content.article.entity;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.i18n.LanguageCode;
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
@Table(name = "articles")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 500, nullable = false)
	private String title;

	@Column(length = 500)
	private String author;

	@Column(length = 2048)
	private String coverImageUrl;

	@Column(length = 2048)
	private String originUrl;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private DifficultyLevel difficultyLevel;

	private Integer readingTime;

	@Column(nullable = false)
	private Double averageRating = 0.0;

	@Column(nullable = false)
	private Integer reviewCount = 0;

	@Column(nullable = false)
	private Integer viewCount = 0;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private ContentCategory category;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<LanguageCode> targetLanguageCode;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

}
