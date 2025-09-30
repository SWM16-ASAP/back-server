package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.ArticleProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleProgressRepository extends MongoRepository<ArticleProgress, String> {
    Optional<ArticleProgress> findByUserIdAndArticleId(String userId, String articleId);
}