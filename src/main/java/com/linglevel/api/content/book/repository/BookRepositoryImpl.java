package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.common.repository.CatalogQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

	private final EntityManager entityManager;

	@Override
	@Transactional(readOnly = true)
	public Page<Book> findBooksWithFilters(GetBooksRequest request, String userId, Pageable pageable) {
		CatalogQuery query = new CatalogQuery().keyword(request.getKeyword())
			.tags(request.getTags())
			.createdAfter(request.getCreatedAfter());
		if (request.getProgress() != null && userId != null) {
			query.progress("BookProgress", "bookId", Long.valueOf(userId), request.getProgress());
		}
		return query.page(entityManager, Book.class, pageable);
	}

}
