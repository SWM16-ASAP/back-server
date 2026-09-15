package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.UserCustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomContentRepository extends JpaRepository<UserCustomContent, Long> {

	Optional<UserCustomContent> findByUserIdAndCustomContentId(Long userId, Long customContentId);

	default Optional<UserCustomContent> findByUserIdAndCustomContentId(String userId, String customContentId) {
		return findByUserIdAndCustomContentId(Long.valueOf(userId), Long.valueOf(customContentId));
	}

	Page<UserCustomContent> findByUserId(Long userId, Pageable pageable);

	List<UserCustomContent> findByUserId(Long userId);

	boolean existsByUserIdAndCustomContentId(Long userId, Long customContentId);

	default boolean existsByUserIdAndCustomContentId(String userId, String customContentId) {
		return existsByUserIdAndCustomContentId(Long.valueOf(userId), Long.valueOf(customContentId));
	}

}
