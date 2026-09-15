package com.linglevel.api.content.book.entity;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chunks")
public class Chunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long chapterId;

	@Column(nullable = false)
	private Integer chunkNumber;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private DifficultyLevel difficultyLevel;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private ChunkType type;

	@Column(columnDefinition = "text", nullable = false)
	private String content;

	@Column(columnDefinition = "text")
	private String description;

	public void updateContent(String content, String description) {
		this.content = content;
		if (description != null) {
			this.description = description;
		}
	}

}
