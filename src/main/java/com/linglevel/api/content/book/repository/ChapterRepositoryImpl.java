package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.ProgressStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RequiredArgsConstructor
public class ChapterRepositoryImpl implements ChapterRepositoryCustom {

	private final EntityManager entityManager;

	private final ObjectProvider<BookProgressRepository> progressRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<Chapter> findChaptersWithFilters(String bookId, GetChaptersRequest request, String userId,
			Pageable pageable) {
		String where = " where c.bookId = :bookId";
		Map<String, Object> params = new HashMap<>();
		params.put("bookId", Long.valueOf(bookId));
		if (userId != null && request.getProgress() != null) {
			BookProgress progress = progressRepository.getObject().findByUserIdAndBookId(userId, bookId).orElse(null);
			List<BookProgress.ChapterProgressInfo> infos = progress == null || progress.getChapterProgresses() == null
					? List.of() : progress.getChapterProgresses();
			List<Integer> numbers = infos.stream().filter(info -> {
				boolean complete = Boolean.TRUE.equals(info.getIsCompleted());
				boolean started = complete
						|| (info.getProgressPercentage() != null && info.getProgressPercentage() > 0);
				return switch (request.getProgress()) {
					case COMPLETED -> complete;
					case IN_PROGRESS -> started && !complete;
					case NOT_STARTED -> started;
				};
			}).map(BookProgress.ChapterProgressInfo::getChapterNumber).distinct().toList();
			boolean exclude = request.getProgress() == ProgressStatus.NOT_STARTED;
			if (numbers.isEmpty() && !exclude)
				return Page.empty(pageable);
			if (!numbers.isEmpty()) {
				where += exclude ? " and c.chapterNumber not in :numbers" : " and c.chapterNumber in :numbers";
				params.put("numbers", numbers);
			}
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
