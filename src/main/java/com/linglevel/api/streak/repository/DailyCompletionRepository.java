package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.DailyCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCompletionRepository extends MongoRepository<DailyCompletion, String> {
    boolean existsByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    Optional<DailyCompletion> findByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    long countByUserId(String userId);

    List<DailyCompletion> findByUserIdAndCompletionDateBetween(String userId, LocalDate startDate, LocalDate endDate);
}