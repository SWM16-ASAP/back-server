package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.FreezeTransaction;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface FreezeTransactionRepository extends MongoRepository<FreezeTransaction, String> {

    Page<FreezeTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByUserIdAndAmountAndCreatedAtBetween(String userId, int amount, Instant start, Instant end);

}
