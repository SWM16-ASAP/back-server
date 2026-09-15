package com.linglevel.api.fcm.scheduler;

import com.linglevel.api.fcm.repository.PushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 180일 이상 지난 푸시 로그 삭제. MongoDB의 {@code createdAt} 기준 TTL 인덱스(180일)를 대체하는 배치 작업이다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushLogCleanupScheduler {

	private static final long RETENTION_DAYS = 180L;

	private final PushLogRepository pushLogRepository;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void deleteOldLogs() {
		try {
			LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
			long deleted = pushLogRepository.deleteByCreatedAtBefore(cutoff);
			log.info("Deleted {} push log(s) created before {}", deleted, cutoff);
		}
		catch (Exception e) {
			log.error("Failed to delete old push logs", e);
		}
	}

}
