package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.UserStudyReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserStudyReportRepository extends MongoRepository<UserStudyReport, String> {
    Optional<UserStudyReport> findByUserId(String userId);

    long countByCurrentStreakLessThan(int currentStreak);

    long countByCurrentStreakGreaterThanEqual(int currentStreak);
}
