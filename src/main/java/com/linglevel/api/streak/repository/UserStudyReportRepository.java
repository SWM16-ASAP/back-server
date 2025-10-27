package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.UserStudyReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStudyReportRepository extends MongoRepository<UserStudyReport, String> {

    /**
     * Find user's study report by userId
     */
    Optional<UserStudyReport> findByUserId(String userId);

    /**
     * Count users with streak greater than or equal to the specified value
     * Used for calculating "Top X%" percentile
     */
    long countByCurrentStreakGreaterThanEqual(int streak);

    /**
     * Check if user study report exists
     */
    boolean existsByUserId(String userId);
}