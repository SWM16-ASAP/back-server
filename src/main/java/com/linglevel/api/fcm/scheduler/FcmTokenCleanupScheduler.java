package com.linglevel.api.fcm.scheduler;

import com.linglevel.api.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 90일 이상 갱신되지 않은 FCM 토큰 삭제. MongoDB의 {@code updatedAt} 기준 TTL 인덱스(90일)를 대체하는 배치 작업이다. 기존
 * TTL과 동일하게 isActive 값과 무관하게 삭제한다 — 90일간 앱을 열지 않은 활성 토큰도 삭제될 수 있는 기존 동작을 그대로 유지한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FcmTokenCleanupScheduler {

	private static final long NINETY_DAYS = 90L;

	private final FcmTokenRepository fcmTokenRepository;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void deleteStaleTokens() {
		try {
			LocalDateTime cutoff = LocalDateTime.now().minusDays(NINETY_DAYS);
			long deleted = fcmTokenRepository.deleteByUpdatedAtBefore(cutoff);
			log.info("Deleted {} stale FCM token(s) not updated since {}", deleted, cutoff);
		}
		catch (Exception e) {
			log.error("Failed to delete stale FCM tokens", e);
		}
	}

}
