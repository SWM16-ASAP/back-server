package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

	default Optional<Book> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

	@Modifying
	@Transactional
	@Query("update Book b set b.viewCount = b.viewCount + 1 where b.id = :id")
	void incrementViewCount(@Param("id") Long id);

	default void incrementViewCount(String id) {
		incrementViewCount(Long.valueOf(id));
	}

}
