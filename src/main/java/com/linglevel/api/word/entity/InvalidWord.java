package com.linglevel.api.word.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalid_words")
public class InvalidWord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String word;

	@Column(nullable = false)
	private LocalDateTime attemptedAt;

	@Builder.Default
	@Column(nullable = false)
	private Integer attemptCount = 1;

}
