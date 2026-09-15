package com.linglevel.api.content.common.repository;

import jakarta.persistence.EntityManager;
import com.linglevel.api.content.common.ProgressStatus;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Shared SQL filters for book and article catalogs. Values are always bound parameters.
 */
public final class CatalogQuery {

	private final StringBuilder where = new StringBuilder(" where 1 = 1");

	private final Map<String, Object> parameters = new LinkedHashMap<>();

	public CatalogQuery keyword(String keyword) {
		if (StringUtils.hasText(keyword)) {
			where.append(" and (lower(c.title) like :keyword escape '!' or lower(c.author) like :keyword escape '!')");
			parameters.put("keyword", "%"
					+ keyword.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
		}
		return this;
	}

	public CatalogQuery tags(String tags) {
		if (StringUtils.hasText(tags)) {
			List<String> values = Arrays.stream(tags.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.distinct()
				.toList();
			if (!values.isEmpty()) {
				List<String> matches = new ArrayList<>();
				for (int i = 0; i < values.size(); i++) {
					String key = "tag" + i;
					matches.add("function('JSON_CONTAINS', c.tags, function('JSON_QUOTE', :" + key + ")) = 1");
					parameters.put(key, values.get(i));
				}
				where.append(" and (").append(String.join(" or ", matches)).append(")");
			}
		}
		return this;
	}

	public CatalogQuery createdAfter(LocalDateTime after) {
		if (after != null) {
			where.append(" and c.createdAt >= :after");
			parameters.put("after", after.toInstant(ZoneOffset.UTC));
		}
		return this;
	}

	public CatalogQuery category(Object category) {
		if (category != null) {
			where.append(" and c.category = :category");
			parameters.put("category", category);
		}
		return this;
	}

	public CatalogQuery language(Enum<?> language) {
		if (language != null) {
			where.append(" and function('JSON_CONTAINS', c.targetLanguageCode, function('JSON_QUOTE', :language)) = 1");
			parameters.put("language", language.name());
		}
		return this;
	}

	public CatalogQuery originsOnly() {
		where.append(" and c.originUrl is not null");
		return this;
	}

	public CatalogQuery progress(String entity, String contentField, Long userId, ProgressStatus status) {
		boolean book = entity.equals("BookProgress");
		String started = book
				? "(p.isCompleted = true or p.normalizedProgress > 0 or exists (select cp.id from BookChapterProgress cp where cp.bookProgress = p and cp.isCompleted = false and cp.progressPercentage > 0))"
				: "p.normalizedProgress > 0";
		String condition = switch (status) {
			case COMPLETED -> " and p.isCompleted = true";
			case IN_PROGRESS -> " and p.isCompleted = false and " + started;
			case NOT_STARTED -> book ? " and " + started : "";
		};
		where.append(status == ProgressStatus.NOT_STARTED ? " and not exists (" : " and exists (")
			.append("select p.id from ")
			.append(entity)
			.append(" p where p.")
			.append(contentField)
			.append(" = c.id and p.userId = :progressUserId")
			.append(condition)
			.append(")");
		parameters.put("progressUserId", userId);
		return this;
	}

	public <T> Page<T> page(EntityManager em, Class<T> type, Pageable pageable) {
		List<String> orders = new ArrayList<>();
		pageable.getSort().forEach(order -> {
			if (!Set.of("createdAt", "viewCount", "averageRating", "id").contains(order.getProperty()))
				throw new IllegalArgumentException("Unsupported catalog sort");
			orders.add("c." + order.getProperty() + (order.isAscending() ? " asc" : " desc"));
		});
		if (pageable.getSort().getOrderFor("id") == null)
			orders.add("c.id desc");
		String from = " from " + em.getMetamodel().entity(type).getName() + " c" + where;
		var data = em.createQuery("select c" + from + " order by " + String.join(", ", orders), type);
		var count = em.createQuery("select count(c)" + from, Long.class);
		parameters.forEach((k, v) -> {
			data.setParameter(k, v);
			count.setParameter(k, v);
		});
		return new PageImpl<>(data.setFirstResult(Math.toIntExact(pageable.getOffset()))
			.setMaxResults(pageable.getPageSize())
			.getResultList(), pageable, count.getSingleResult());
	}

}
