package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.*;

@RequiredArgsConstructor
public class CustomContentRepositoryImpl implements CustomContentRepositoryCustom {

	private final EntityManager entityManager;

	private final org.springframework.beans.factory.ObjectProvider<CustomContentProgressRepository> progressRepository;

	@Override
	public Page<CustomContent> findCustomContentsWithFilters(String userId, GetCustomContentsRequest request,
			Pageable pageable) {
		return findCustomContentsByUserWithFilters(userId, request, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CustomContent> findCustomContentsByUserWithFilters(String userId, GetCustomContentsRequest request,
			Pageable pageable) {
		StringBuilder where = new StringBuilder(
				" where c.isDeleted = false and exists (select u.id from UserCustomContent u where u.customContentId = c.id and u.userId = :userId)");
		Map<String, Object> params = new HashMap<>();
		params.put("userId", Long.valueOf(userId));
		if (StringUtils.hasText(request.getKeyword())) {
			where.append(" and (lower(c.title) like :keyword escape '!' or lower(c.author) like :keyword escape '!')");
			params.put("keyword",
					"%" + request.getKeyword()
						.toLowerCase(Locale.ROOT)
						.replace("!", "!!")
						.replace("%", "!%")
						.replace("_", "!_") + "%");
		}
		if (StringUtils.hasText(request.getTags())) {
			int index = 0;
			for (String tag : request.getTags().split(",")) {
				String name = "tag" + index++;
				where.append(" and function('JSON_CONTAINS', c.tags, function('JSON_QUOTE', :")
					.append(name)
					.append(")) = 1");
				params.put(name, tag.trim());
			}
		}
		if (request.getProgress() != null) {
			List<CustomContentProgress> progress = progressRepository.getObject().findAllByUserId(userId);
			List<Long> ids = progress.stream().filter(p -> switch (request.getProgress()) {
				case NOT_STARTED -> true;
				case COMPLETED -> Boolean.TRUE.equals(p.getIsCompleted());
				case IN_PROGRESS -> !Boolean.TRUE.equals(p.getIsCompleted()) && p.getNormalizedProgress() != null
						&& p.getNormalizedProgress() > 0;
			}).map(CustomContentProgress::getCustomId).map(Long::valueOf).toList();
			if (ids.isEmpty() && request.getProgress() != com.linglevel.api.content.common.ProgressStatus.NOT_STARTED) {
				return Page.empty(pageable);
			}
			if (!ids.isEmpty()) {
				where.append(request.getProgress() == com.linglevel.api.content.common.ProgressStatus.NOT_STARTED
						? " and c.id not in :progressIds" : " and c.id in :progressIds");
				params.put("progressIds", ids);
			}
		}
		List<String> sort = new ArrayList<>();
		pageable.getSort().forEach(order -> {
			if (!Set.of("createdAt", "viewCount", "averageRating", "id").contains(order.getProperty())) {
				throw new IllegalArgumentException("Unsupported content sort");
			}
			sort.add("c." + order.getProperty() + (order.isAscending() ? " asc" : " desc"));
		});
		sort.add("c.id desc");
		TypedQuery<CustomContent> query = entityManager.createQuery(
				"select c from CustomContent c" + where + " order by " + String.join(", ", sort), CustomContent.class);
		TypedQuery<Long> count = entityManager.createQuery("select count(c) from CustomContent c" + where, Long.class);
		params.forEach((key, value) -> {
			query.setParameter(key, value);
			count.setParameter(key, value);
		});
		return new PageImpl<>(query.setFirstResult(Math.toIntExact(pageable.getOffset()))
			.setMaxResults(pageable.getPageSize())
			.getResultList(), pageable, count.getSingleResult());
	}

	@Override
	@Transactional
	public void incrementViewCount(String customContentId) {
		entityManager
			.createQuery(
					"update CustomContent c set c.viewCount = c.viewCount + 1 where c.id = :id and c.isDeleted = false")
			.setParameter("id", Long.valueOf(customContentId))
			.executeUpdate();
	}

}
