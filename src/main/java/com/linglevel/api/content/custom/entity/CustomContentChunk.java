package com.linglevel.api.content.custom.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "custom_content_chunks")
public class CustomContentChunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "custom_id", nullable = false)
	private Long customContentId;

	@Column(nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private DifficultyLevel difficultyLevel;

	@Column(nullable = false)
	private Integer chapterNum;

	@Column(nullable = false)
	private Integer chunkNum;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private ChunkType type;

	@Column(columnDefinition = "text", nullable = false)
	private String chunkText;

	@Column(columnDefinition = "text")
	private String description;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isDeleted = false;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	private Instant deletedAt;

}
