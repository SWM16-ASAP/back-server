package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.UserStudyReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface UserStudyReportRepository extends JpaRepository<UserStudyReport, Long> {

	Optional<UserStudyReport> findByUserId(Long userId);

	default Optional<UserStudyReport> findByUserId(String id) {
		return findByUserId(Long.valueOf(id));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from UserStudyReport r where r.userId = :id")
	Optional<UserStudyReport> findForUpdate(@Param("id") Long id);

	default Optional<UserStudyReport> findForUpdate(String id) {
		return findForUpdate(Long.valueOf(id));
	}

	long countByCurrentStreakGreaterThanEqual(int count);

	List<UserStudyReport> findByCurrentStreakGreaterThan(int count);

	@Query("select r from UserStudyReport r where r.currentStreak = 0 and r.lastLearningTimestamp >= :start and r.lastLearningTimestamp < :end")
	List<UserStudyReport> findChurnedUsersInTimeWindow(@Param("start") Instant start, @Param("end") Instant end);

}
