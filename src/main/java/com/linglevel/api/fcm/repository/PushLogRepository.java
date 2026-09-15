package com.linglevel.api.fcm.repository;

import com.linglevel.api.fcm.entity.PushLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushLogRepository extends JpaRepository<PushLog, Long> {

	Optional<PushLog> findByCampaignId(String campaignId);

	List<PushLog> findByCampaignGroup(String campaignGroup);

	List<PushLog> findByUserId(Long userId);

	default List<PushLog> findByUserId(String userId) {
		return findByUserId(Long.valueOf(userId));
	}

	List<PushLog> findBySentAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	@Transactional
	long deleteByCreatedAtBefore(LocalDateTime cutoff);

}
