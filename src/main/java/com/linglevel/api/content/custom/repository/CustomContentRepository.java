package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CustomContentRepository extends JpaRepository<CustomContent, Long>, CustomContentRepositoryCustom {

	Page<CustomContent> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

	Optional<CustomContent> findByIdAndIsDeletedFalse(Long id);

	default Optional<CustomContent> findByIdAndIsDeletedFalse(String id) {
		return findByIdAndIsDeletedFalse(Long.valueOf(id));
	}

	default Optional<CustomContent> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

	Optional<CustomContent> findByOriginUrlAndIsDeletedFalse(String originUrl);

	@Modifying
	@Transactional
	@Query("update CustomContent c set c.viewCount = c.viewCount + 1 where c.id = :id and c.isDeleted = false")
	void incrementViewCount(@Param("id") Long id);

	default void incrementViewCount(String id) {
		incrementViewCount(Long.valueOf(id));
	}

}
