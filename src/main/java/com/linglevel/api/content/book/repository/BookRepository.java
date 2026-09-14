package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

	default Optional<Book> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

}
