package com.linglevel.api.crawling.entity;

import com.linglevel.api.content.feed.entity.FeedContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "crawling_dsl")
public class CrawlingDsl {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 255, nullable = false)
	private String domain;

	@Column(length = 500, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private FeedContentType contentType;

	@Column(columnDefinition = "text", nullable = false)
	private String titleDsl;

	@Column(columnDefinition = "text", nullable = false)
	private String contentDsl;

	@Column(columnDefinition = "text")
	private String coverImageDsl;

	@Column(length = 2048)
	private String accessUrl;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

}
