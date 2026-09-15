package com.linglevel.api.bookmark.entity;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "word_bookmarks")
public class WordBookmark {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 255)
	private String word;

	@Builder.Default
	@Column(nullable = false)
	private LocalDateTime bookmarkedAt = LocalDateTime.now();

}
