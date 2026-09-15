package com.linglevel.api.version.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_version")
public class AppVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50, nullable = false)
	private String latestVersion;

	@Column(length = 50, nullable = false)
	private String minimumVersion;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

}
