package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long>, ChapterRepositoryCustom {

	Page<Chapter> findByBookId(Long chapterId, Pageable pageable);

	List<Chapter> findByBookIdOrderByChapterNumber(Long bookId);

	Optional<Chapter> findByBookIdAndChapterNumber(Long bookId, int chapterNumber);

	Optional<Chapter> findFirstByBookIdOrderByChapterNumberAsc(Long chapterId);

	Integer countByBookId(Long bookId);

	Optional<Chapter> findById(Long chapterId);

	default Optional<Chapter> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

	default Page<Chapter> findByBookId(String id, Pageable pageable) {
		return findByBookId(Long.valueOf(id), pageable);
	}

	default List<Chapter> findByBookIdOrderByChapterNumber(String id) {
		return findByBookIdOrderByChapterNumber(Long.valueOf(id));
	}

	default Optional<Chapter> findByBookIdAndChapterNumber(String id, int number) {
		return findByBookIdAndChapterNumber(Long.valueOf(id), number);
	}

	default Optional<Chapter> findFirstByBookIdOrderByChapterNumberAsc(String id) {
		return findFirstByBookIdOrderByChapterNumberAsc(Long.valueOf(id));
	}

	default Integer countByBookId(String id) {
		return countByBookId(Long.valueOf(id));
	}

}
