package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}
