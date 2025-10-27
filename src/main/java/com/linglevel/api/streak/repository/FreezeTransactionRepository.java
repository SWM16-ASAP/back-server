package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.FreezeTransaction;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.MongoRepository;



public interface FreezeTransactionRepository extends MongoRepository<FreezeTransaction, String> {

    Page<FreezeTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

}
