package com.linglevel.api.auth.scheduler;

import com.linglevel.api.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 만료된 refresh token 정리. MongoDB TTL 인덱스를 대체하는 배치 작업이다. 만료 여부는
 * {@link com.linglevel.api.auth.jwt.RefreshToken#isExpired()}로 읽기 시점에도 확인하므로, 이 스케줄러는 누적을
 * 막기 위한 정리 용도다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

	private final RefreshTokenRepository refreshTokenRepository;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void deleteExpiredTokens() {
		try {
			long deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
			log.info("Deleted {} expired refresh token(s)", deleted);
		}
		catch (Exception e) {
			log.error("Failed to delete expired refresh tokens", e);
		}
	}

}
