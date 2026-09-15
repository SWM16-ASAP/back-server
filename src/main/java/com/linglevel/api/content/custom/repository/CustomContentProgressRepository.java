package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface CustomContentProgressRepository extends JpaRepository<CustomContentProgress, Long> {

	Optional<CustomContentProgress> findByUserIdAndCustomId(Long userId, Long customId);

	default Optional<CustomContentProgress> findByUserIdAndCustomId(String userId, String customId) {
		return findByUserIdAndCustomId(Long.valueOf(userId), Long.valueOf(customId));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from CustomContentProgress p where p.userId = :userId and p.customId = :contentId")
	Optional<CustomContentProgress> findForUpdate(Long userId, Long contentId);

	default Optional<CustomContentProgress> findForUpdate(String userId, String contentId) {
		return findForUpdate(Long.valueOf(userId), Long.valueOf(contentId));
	}

	List<CustomContentProgress> findAllByUserId(Long userId);

	default List<CustomContentProgress> findAllByUserId(String userId) {
		return findAllByUserId(Long.valueOf(userId));
	}

}
