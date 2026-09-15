package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.ProgressStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RequiredArgsConstructor
public class ChapterRepositoryImpl implements ChapterRepositoryCustom {

	private final EntityManager entityManager;

	@Override
	@Transactional(readOnly = true)
	public Page<Chapter> findChaptersWithFilters(String bookId, GetChaptersRequest request, String userId,
			Pageable pageable) {
		String where = " where c.bookId = :bookId";
		Map<String, Object> params = new HashMap<>();
		params.put("bookId", Long.valueOf(bookId));
		if (userId != null && request.getProgress() != null) {
			String condition = switch (request.getProgress()) {
				case COMPLETED -> "cp.isCompleted = true";
				case IN_PROGRESS -> "cp.isCompleted = false and cp.progressPercentage > 0";
				case NOT_STARTED -> "(cp.isCompleted = true or cp.progressPercentage > 0)";
			};
			where += request.getProgress() == ProgressStatus.NOT_STARTED ? " and not exists (" : " and exists (";
			where += "select cp.id from BookChapterProgress cp join cp.bookProgress p where p.bookId = c.bookId and p.userId = :userId and cp.chapterNumber = c.chapterNumber and "
					+ condition + ")";
			params.put("userId", Long.valueOf(userId));
		}
		List<String> orders = new ArrayList<>();
		pageable.getSort().forEach(order -> {
			if (!Set.of("chapterNumber", "id").contains(order.getProperty()))
				throw new IllegalArgumentException("Unsupported chapter sort");
			orders.add("c." + order.getProperty() + (order.isAscending() ? " asc" : " desc"));
		});
		if (orders.isEmpty())
			orders.add("c.chapterNumber asc");
		if (pageable.getSort().getOrderFor("id") == null)
			orders.add("c.id asc");
		var query = entityManager
			.createQuery("select c from Chapter c" + where + " order by " + String.join(", ", orders), Chapter.class);
		var count = entityManager.createQuery("select count(c) from Chapter c" + where, Long.class);
		params.forEach((k, v) -> {
			query.setParameter(k, v);
			count.setParameter(k, v);
		});
		return new PageImpl<>(query.setFirstResult(Math.toIntExact(pageable.getOffset()))
			.setMaxResults(pageable.getPageSize())
			.getResultList(), pageable, count.getSingleResult());
	}

}
