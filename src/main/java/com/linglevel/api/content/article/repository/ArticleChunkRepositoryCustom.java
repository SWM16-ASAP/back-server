package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.repository.dto.ArticleChunkCount;
import java.util.List;

public interface ArticleChunkRepositoryCustom {
    List<ArticleChunkCount> countChunksByArticleIds(List<String> articleIds);
}
