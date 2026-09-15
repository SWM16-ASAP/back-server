package com.linglevel.api.auth.jwt;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 36)
	private String tokenId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	public boolean isExpired() {
		return expiresAt.isBefore(LocalDateTime.now());
	}

}
