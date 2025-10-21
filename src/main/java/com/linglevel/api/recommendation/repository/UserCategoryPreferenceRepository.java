package com.linglevel.api.recommendation.repository;

import com.linglevel.api.recommendation.entity.UserCategoryPreference;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserCategoryPreferenceRepository extends MongoRepository<UserCategoryPreference, String> {

    Optional<UserCategoryPreference> findByUserId(String userId);
}
