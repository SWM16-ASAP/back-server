package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.ChunkCountByLevelDto;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

	Page<Chunk> findByChapterIdAndDifficultyLevel(Long chapterId, DifficultyLevel difficultyLevel, Pageable pageable);

	default Page<Chunk> findByChapterIdAndDifficultyLevel(String chapterId, DifficultyLevel difficultyLevel,
			Pageable pageable) {
		return findByChapterIdAndDifficultyLevel(Long.valueOf(chapterId), difficultyLevel, pageable);
	}

	Optional<Chunk> findFirstByChapterIdOrderByChunkNumberAsc(Long chapterId);

	default Optional<Chunk> findFirstByChapterIdOrderByChunkNumberAsc(String chapterId) {
		return findFirstByChapterIdOrderByChunkNumberAsc(Long.valueOf(chapterId));
	}

	Optional<Chunk> findById(Long chunkId);

	default Optional<Chunk> findById(String chunkId) {
		return findById(Long.valueOf(chunkId));
	}

	default boolean existsById(String chunkId) {
		return existsById(Long.valueOf(chunkId));
	}

	List<Chunk> findByChapterIdOrderByChunkNumber(Long chapterId);

	default List<Chunk> findByChapterIdOrderByChunkNumber(String chapterId) {
		return findByChapterIdOrderByChunkNumber(Long.valueOf(chapterId));
	}

	long countByChapterIdAndDifficultyLevel(Long chapterId, DifficultyLevel difficultyLevel);

	default long countByChapterIdAndDifficultyLevel(String chapterId, DifficultyLevel difficultyLevel) {
		return countByChapterIdAndDifficultyLevel(Long.valueOf(chapterId), difficultyLevel);
	}

	@Query("""
			select new com.linglevel.api.content.book.dto.ChunkCountByLevelDto(cast(c.chapterId as string), c.difficultyLevel, count(c))
			from Chunk c
			where c.chapterId in :chapterIds
			group by c.chapterId, c.difficultyLevel
			""")
	List<ChunkCountByLevelDto> findChunkCountsByChapterIdsInternal(@Param("chapterIds") List<Long> chapterIds);

	default List<ChunkCountByLevelDto> findChunkCountsByChapterIds(List<String> chapterIds) {
		if (chapterIds.isEmpty()) {
			return List.of();
		}
		List<Long> ids = chapterIds.stream().map(Long::valueOf).collect(Collectors.toList());
		return findChunkCountsByChapterIdsInternal(ids);
	}

}
