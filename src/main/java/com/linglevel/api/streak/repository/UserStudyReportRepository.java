package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.UserStudyReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserStudyReportRepository extends MongoRepository<UserStudyReport, String> {
    Optional<UserStudyReport> findByUserId(String userId);

    long countByCurrentStreakGreaterThanEqual(int currentStreak);

    List<UserStudyReport> findByCurrentStreakGreaterThan(int currentStreak);

    /**
     * 이탈 유저 복귀 알림을 위한 사용자 조회 (currentStreak = 0)
     * 마지막 학습 시간이 특정 범위 내에 있는 이탈 유저를 찾습니다.
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 조건을 만족하는 이탈 유저 리포트 목록
     */
    @Query("{ 'currentStreak': 0, 'lastLearningTimestamp': { $gte: ?0, $lt: ?1 } }")
    List<UserStudyReport> findChurnedUsersInTimeWindow(Instant startTime, Instant endTime);
}
