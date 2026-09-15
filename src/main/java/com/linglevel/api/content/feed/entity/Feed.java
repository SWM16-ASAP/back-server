package com.linglevel.api.content.feed.entity;

import com.linglevel.api.content.common.ContentCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feeds")
public class Feed {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private FeedContentType contentType;

	@Column(length = 500, nullable = false)
	private String title;

	@Column(length = 2048, nullable = false)
	private String url;

	@Column(length = 2048)
	private String thumbnailUrl;

	@Column(length = 500)
	private String author;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ContentCategory category;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@Column(length = 255)
	private String sourceProvider;

	private Instant publishedAt;

	private Integer displayOrder;

	@Builder.Default
	@Column(nullable = false)
	private Integer viewCount = 0;

	private Double avgReadTimeSeconds;

	@Column(nullable = false)
	private Instant createdAt;

	@Builder.Default
	@Column(nullable = false)
	private Boolean deleted = false;

	private Instant deletedAt;

}
