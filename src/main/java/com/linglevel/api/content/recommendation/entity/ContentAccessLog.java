package com.linglevel.api.content.recommendation.entity;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_access_logs")
public class ContentAccessLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long contentId;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private ContentType contentType;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ContentCategory category;

	private Integer readTimeSeconds;

	@Column(nullable = false)
	private Instant accessedAt;

}
