package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.BookProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BookProgressRepository extends JpaRepository<BookProgress, Long> {

	Optional<BookProgress> findByUserIdAndBookId(Long userId, Long bookId);

	default Optional<BookProgress> findByUserIdAndBookId(String userId, String bookId) {
		return findByUserIdAndBookId(Long.valueOf(userId), Long.valueOf(bookId));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from BookProgress p where p.userId = :userId and p.bookId = :contentId")
	Optional<BookProgress> findForUpdate(Long userId, Long contentId);

	default Optional<BookProgress> findForUpdate(String userId, String contentId) {
		return findForUpdate(Long.valueOf(userId), Long.valueOf(contentId));
	}

	List<BookProgress> findAllByUserId(Long userId);

	default List<BookProgress> findAllByUserId(String userId) {
		return findAllByUserId(Long.valueOf(userId));
	}

	List<BookProgress> findByUserIdAndBookIdIn(Long userId, List<Long> bookIds);

	default List<BookProgress> findByUserIdAndBookIdIn(String userId, List<String> bookIds) {
		return findByUserIdAndBookIdIn(Long.valueOf(userId), bookIds.stream().map(Long::valueOf).toList());
	}

	Page<BookProgress> findAllByUserId(Long userId, Pageable pageable);

	default Page<BookProgress> findAllByUserId(String userId, Pageable pageable) {
		return findAllByUserId(Long.valueOf(userId), pageable);
	}

	List<BookProgress> findByBookId(Long bookId);

	default List<BookProgress> findByBookId(String bookId) {
		return findByBookId(Long.valueOf(bookId));
	}

}
