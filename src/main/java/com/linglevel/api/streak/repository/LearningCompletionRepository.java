package com.linglevel.api.streak.repository;

import com.linglevel.api.streak.entity.LearningCompletion;
import com.linglevel.api.content.common.ContentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LearningCompletionRepository extends JpaRepository<LearningCompletion, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<LearningCompletion> findFirstByDailyCompletionUserIdAndTypeAndContentId(Long userId, ContentType type,
			Long contentId);

	@Query(value = "select count(distinct l.content_type, l.content_id) from learning_completions l join daily_completions d on d.id = l.daily_completion_id where d.user_id = :userId",
			nativeQuery = true)
	long countDistinctContents(@Param("userId") Long userId);

}
