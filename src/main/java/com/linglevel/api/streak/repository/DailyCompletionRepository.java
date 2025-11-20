package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.DailyCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCompletionRepository extends MongoRepository<DailyCompletion, String> {
    boolean existsByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    Optional<DailyCompletion> findByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    Optional<DailyCompletion> findTopByUserIdAndCompletionDateBeforeOrderByCompletionDateDesc(String userId, LocalDate date);

    long countByUserId(String userId);

    /**
     * 경계값을 포함하는 범위 조회 ($gte, $lte 사용)
     * Between은 $gt, $lt를 사용하여 경계값을 제외하므로 커스텀 쿼리 사용
     */
    @Query("{ 'userId': ?0, 'completionDate': { $gte: ?1, $lte: ?2 } }")
    List<DailyCompletion> findByUserIdAndCompletionDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * 최근 N일간의 학습 기록 조회 (학습 시간대 분석용)
     */
    @Query("{ 'userId': ?0, 'completionDate': { $gte: ?1 } }")
    List<DailyCompletion> findByUserIdAndCompletionDateAfter(String userId, LocalDate startDate);

    /**
     * 특정 사용자의 모든 DailyCompletion을 날짜 오름차순으로 조회
     */
    List<DailyCompletion> findByUserIdOrderByCompletionDateAsc(String userId);
}