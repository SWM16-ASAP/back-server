package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArticleRepository extends MongoRepository<Article, String>, ArticleRepositoryCustom {

}
