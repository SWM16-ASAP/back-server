package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.DailyCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface DailyCompletionRepository extends MongoRepository<DailyCompletion, String> {
    boolean existsByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    long countByUserId(String userId);
}