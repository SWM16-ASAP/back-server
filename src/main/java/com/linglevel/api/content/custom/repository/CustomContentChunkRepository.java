package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomContentChunkRepository extends JpaRepository<CustomContentChunk, Long> {

	List<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			Long customContentId);

	default List<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			String customContentId) {
		return findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(Long.valueOf(customContentId));
	}

	List<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			Long customContentId, DifficultyLevel difficultyLevel);

	default List<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			String customContentId, DifficultyLevel difficultyLevel) {
		return findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
				Long.valueOf(customContentId), difficultyLevel);
	}

	Page<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			Long customContentId, DifficultyLevel difficultyLevel, Pageable pageable);

	default Page<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			String customContentId, DifficultyLevel difficultyLevel, Pageable pageable) {
		return findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
				Long.valueOf(customContentId), difficultyLevel, pageable);
	}

	List<CustomContentChunk> findByUserIdAndIsDeletedFalse(Long userId);

	default List<CustomContentChunk> findByUserIdAndIsDeletedFalse(String userId) {
		return findByUserIdAndIsDeletedFalse(Long.valueOf(userId));
	}

	Page<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(Long customContentId,
			Pageable pageable);

	default Page<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			String customContentId, Pageable pageable) {
		return findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(Long.valueOf(customContentId),
				pageable);
	}

	Optional<CustomContentChunk> findByIdAndCustomContentIdAndIsDeletedFalse(Long id, Long customContentId);

	default Optional<CustomContentChunk> findByIdAndCustomContentIdAndIsDeletedFalse(String id,
			String customContentId) {
		return findByIdAndCustomContentIdAndIsDeletedFalse(Long.valueOf(id), Long.valueOf(customContentId));
	}

	Optional<CustomContentChunk> findFirstByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			Long customContentId);

	default Optional<CustomContentChunk> findFirstByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
			String customContentId) {
		return findFirstByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
				Long.valueOf(customContentId));
	}

	Optional<CustomContentChunk> findById(Long id);

	default Optional<CustomContentChunk> findById(String id) {
		return findById(Long.valueOf(id));
	}

	long countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(Long customContentId,
			DifficultyLevel difficultyLevel);

	default long countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(String customContentId,
			DifficultyLevel difficultyLevel) {
		return countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(Long.valueOf(customContentId),
				difficultyLevel);
	}

}
