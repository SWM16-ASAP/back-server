package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.common.repository.CatalogQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

	private final EntityManager entityManager;

	private final ObjectProvider<ArticleProgressRepository> progressRepository;

	@Override
	@Transactional(readOnly = true)
	public Page<Article> findArticlesWithFilters(GetArticlesRequest request, String userId, Pageable pageable) {
		CatalogQuery query = new CatalogQuery().keyword(request.getKeyword())
			.tags(request.getTags())
			.createdAfter(request.getCreatedAfter())
			.category(request.getCategory())
			.language(request.getTargetLanguageCode());
		if (request.getProgress() != null && userId != null) {
			List<Long> ids = progressRepository.getObject()
				.findAllByUserId(userId)
				.stream()
				.filter(p -> switch (request.getProgress()) {
					case NOT_STARTED -> true;
					case COMPLETED -> Boolean.TRUE.equals(p.getIsCompleted());
					case IN_PROGRESS -> !Boolean.TRUE.equals(p.getIsCompleted()) && p.getNormalizedProgress() != null
							&& p.getNormalizedProgress() > 0;
				})
				.map(ArticleProgress::getArticleId)
				.map(Long::valueOf)
				.toList();
			query.ids(ids, request.getProgress() == ProgressStatus.NOT_STARTED);
		}
		return query.page(entityManager, Article.class, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Article> findArticleOriginsWithFilters(GetArticleOriginsRequest request, Pageable pageable) {
		return new CatalogQuery().originsOnly()
			.tags(request.getTags())
			.category(request.getCategoryEnum())
			.language(request.getTargetLanguageCode())
			.page(entityManager, Article.class, pageable);
	}

	@Override
	@Transactional
	public void incrementViewCount(String id) {
		entityManager.createQuery("update Article c set c.viewCount = c.viewCount + 1 where c.id = :id")
			.setParameter("id", Long.valueOf(id))
			.executeUpdate();
	}

}
