package com.linglevel.api.fcm.scheduler;

import com.linglevel.api.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 90일 이상 갱신되지 않은 비활성 FCM 토큰 삭제. MongoDB의 {@code updatedAt} 기준 TTL 인덱스(90일)를 대체하는 배치 작업이다.
 * Mongo TTL은 {@code isActive} 값과 무관하게 삭제했지만, 그건 저장소 구현상의 제약이었을 뿐 의도한 정책은 아니었다 — 여전히 로그인
 * 중인 사용자의 토큰까지 지워 조용히 푸시를 못 받게 만들 이유가 없으므로, MySQL로 옮기면서 활성 토큰은 삭제 대상에서 제외하도록 정리했다.
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
			long deleted = fcmTokenRepository.deleteByUpdatedAtBeforeAndIsActive(cutoff, false);
			log.info("Deleted {} stale inactive FCM token(s) not updated since {}", deleted, cutoff);
		}
		catch (Exception e) {
			log.error("Failed to delete stale FCM tokens", e);
		}
	}

}
