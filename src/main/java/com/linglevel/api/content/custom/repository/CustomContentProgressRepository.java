package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContentProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomContentProgressRepository extends MongoRepository<CustomContentProgress, String> {
    Optional<CustomContentProgress> findByUserIdAndCustomId(String userId, String customId);
}