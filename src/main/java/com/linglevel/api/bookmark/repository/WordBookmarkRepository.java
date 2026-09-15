package com.linglevel.api.bookmark.repository;

import com.linglevel.api.bookmark.entity.WordBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WordBookmarkRepository extends JpaRepository<WordBookmark, Long> {

	boolean existsByUserIdAndWord(Long userId, String word);

	default boolean existsByUserIdAndWord(String userId, String word) {
		return existsByUserIdAndWord(Long.valueOf(userId), word);
	}

	Page<WordBookmark> findByUserId(Long userId, Pageable pageable);

	Page<WordBookmark> findByUserIdAndWordContainingIgnoreCase(Long userId, String word, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from WordBookmark b where b.userId = :userId and b.word = :word")
	Optional<WordBookmark> findForUpdate(Long userId, String word);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from WordBookmark b where b.userId = :userId and b.id = :id")
	Optional<WordBookmark> findForUpdateById(Long userId, Long id);

}
