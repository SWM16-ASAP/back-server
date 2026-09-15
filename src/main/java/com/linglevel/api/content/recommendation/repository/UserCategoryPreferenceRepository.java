package com.linglevel.api.content.recommendation.repository;

import com.linglevel.api.content.recommendation.entity.UserCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCategoryPreferenceRepository extends JpaRepository<UserCategoryPreference, Long> {

	Optional<UserCategoryPreference> findByUserId(Long userId);

	default Optional<UserCategoryPreference> findByUserId(String userId) {
		return findByUserId(Long.valueOf(userId));
	}

}
