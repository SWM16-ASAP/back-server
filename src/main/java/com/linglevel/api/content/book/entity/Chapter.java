package com.linglevel.api.content.book.entity;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chapters")
public class Chapter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long bookId;

	@Column(nullable = false)
	private Integer chapterNumber;

	@Column(length = 500, nullable = false)
	private String title;

	@Column(length = 2048)
	private String chapterImageUrl;

	@Column(columnDefinition = "text")
	private String description;

	private Integer readingTime;

}
