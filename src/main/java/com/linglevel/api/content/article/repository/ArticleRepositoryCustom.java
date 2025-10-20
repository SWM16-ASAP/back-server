package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.dto.GetArticleOriginsRequest;
import com.linglevel.api.content.article.dto.GetArticlesRequest;
import com.linglevel.api.content.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleRepositoryCustom {
    Page<Article> findArticlesWithFilters(GetArticlesRequest request, String userId, Pageable pageable);

    Page<Article> findArticleOriginsWithFilters(GetArticleOriginsRequest request, Pageable pageable);

    void incrementViewCount(String articleId);
}
