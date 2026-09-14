package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.common.repository.CatalogQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

	private final EntityManager entityManager;

	private final ObjectProvider<BookProgressRepository> progressRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<Book> findBooksWithFilters(GetBooksRequest request, String userId, Pageable pageable) {
		CatalogQuery query = new CatalogQuery().keyword(request.getKeyword())
			.tags(request.getTags())
			.createdAfter(request.getCreatedAfter());
		if (request.getProgress() != null && userId != null) {
			List<Long> ids = progressRepository.getObject()
				.findAllByUserId(userId)
				.stream()
				.filter(p -> switch (request.getProgress()) {
					case NOT_STARTED -> started(p);
					case COMPLETED -> Boolean.TRUE.equals(p.getIsCompleted());
					case IN_PROGRESS -> !Boolean.TRUE.equals(p.getIsCompleted()) && started(p);
				})
				.map(BookProgress::getBookId)
				.map(Long::valueOf)
				.toList();
			query.ids(ids, request.getProgress() == ProgressStatus.NOT_STARTED);
		}
		return query.page(entityManager, Book.class, pageable);
	}

	private boolean started(BookProgress p) {
		return Boolean.TRUE.equals(p.getIsCompleted())
				|| (p.getNormalizedProgress() != null && p.getNormalizedProgress() > 0)
				|| (p.getChapterProgresses() != null && p.getChapterProgresses()
					.stream()
					.anyMatch(c -> !Boolean.TRUE.equals(c.getIsCompleted()) && c.getProgressPercentage() != null
							&& c.getProgressPercentage() > 0));
	}

	@Override
	@Transactional
	public void incrementViewCount(String id) {
		entityManager.createQuery("update Book c set c.viewCount = c.viewCount + 1 where c.id = :id")
			.setParameter("id", Long.valueOf(id))
			.executeUpdate();
	}

}
