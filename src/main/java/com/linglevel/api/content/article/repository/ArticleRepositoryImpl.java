package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.common.repository.CatalogQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

	private final EntityManager entityManager;

	@Override
	@Transactional(readOnly = true)
	public Page<Article> findArticlesWithFilters(GetArticlesRequest request, String userId, Pageable pageable) {
		CatalogQuery query = new CatalogQuery().keyword(request.getKeyword())
			.tags(request.getTags())
			.createdAfter(request.getCreatedAfter())
			.category(request.getCategory())
			.language(request.getTargetLanguageCode());
		if (request.getProgress() != null && userId != null) {
			query.progress("ArticleProgress", "articleId", Long.valueOf(userId), request.getProgress());
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

}
