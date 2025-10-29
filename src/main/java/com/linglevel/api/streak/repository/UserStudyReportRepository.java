package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.UserStudyReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserStudyReportRepository extends MongoRepository<UserStudyReport, String> {
    Optional<UserStudyReport> findByUserId(String userId);

    long countByCurrentStreakLessThan(int currentStreak);

    long countByCurrentStreakGreaterThanEqual(int currentStreak);

    List<UserStudyReport> findByCurrentStreakGreaterThan(int currentStreak);

    /**
     * 최적 타이밍 알림을 위한 사용자 조회
     * lastLearningTimestamp가 특정 시간 범위 내에 있고, 활성 스트릭을 가진 사용자를 찾습니다.
     *
     * @param startTime 시작 시간 (23시간 전)
     * @param endTime 종료 시간 (24시간 전)
     * @return 해당 조건을 만족하는 사용자 리포트 목록
     */
    @Query("{ 'lastLearningTimestamp': { $gte: ?0, $lt: ?1 }, 'currentStreak': { $gt: 0 } }")
    List<UserStudyReport> findUsersForOptimalTimingReminder(Instant startTime, Instant endTime);
}
