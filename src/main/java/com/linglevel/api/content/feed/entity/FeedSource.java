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
@Table(name = "feed_sources")
public class FeedSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 2048, nullable = false)
	private String url;

	@Column(length = 255)
	private String domain;

	@Column(length = 500, nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String coverImageDsl;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private FeedContentType contentType;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ContentCategory category;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<String> tags;

	@Column(nullable = false)
	private Boolean isActive;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

}
