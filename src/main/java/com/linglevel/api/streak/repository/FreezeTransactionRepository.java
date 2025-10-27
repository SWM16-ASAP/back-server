package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.FreezeTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FreezeTransactionRepository extends MongoRepository<FreezeTransaction, String> {

    /**
     * Find all freeze transactions for a user, ordered by creation date (newest first)
     */
    List<FreezeTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find freeze consumption records (amount < 0) within a date range
     * Used for calendar API to identify freeze-covered dates
     */
    @Query("{ 'userId': ?0, 'amount': { $lt: 0 }, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    List<FreezeTransaction> findConsumptionsByUserIdAndDateRange(
            String userId,
            Instant startDate,
            Instant endDate
    );

    /**
     * Find all transactions within a date range
     */
    List<FreezeTransaction> findByUserIdAndCreatedAtBetween(
            String userId,
            Instant startDate,
            Instant endDate
    );
}
