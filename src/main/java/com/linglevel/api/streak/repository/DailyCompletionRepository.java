package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.DailyCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCompletionRepository extends MongoRepository<DailyCompletion, String> {


    Optional<DailyCompletion> findByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    boolean existsByUserIdAndCompletionDate(String userId, LocalDate completionDate);

    List<DailyCompletion> findByUserIdAndCompletionDateBetween(
            String userId,
            LocalDate startDate,
            LocalDate endDate
    );


    long countByUserId(String userId);


    @Query(value = "{ 'userId': ?0 }", fields = "{ 'completionCount': 1 }")
    List<DailyCompletion> findCompletionCountsByUserId(String userId);
}
