package com.linglevel.api.content.recommendation.repository;

import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface ContentAccessLogRepository extends JpaRepository<ContentAccessLog, Long> {

	List<ContentAccessLog> findByAccessedAtAfter(Instant after);

	@Transactional
	void deleteByAccessedAtBefore(Instant before);

}
