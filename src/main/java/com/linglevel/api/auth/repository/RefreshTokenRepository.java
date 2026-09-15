package com.linglevel.api.auth.repository;

import com.linglevel.api.auth.jwt.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenId(String tokenId);

	Optional<RefreshToken> findByUserId(Long userId);

	default Optional<RefreshToken> findByUserId(String userId) {
		return findByUserId(Long.valueOf(userId));
	}

	void deleteByUserId(Long userId);

	default void deleteByUserId(String userId) {
		deleteByUserId(Long.valueOf(userId));
	}

	long deleteByExpiresAtBefore(LocalDateTime cutoff);

}
