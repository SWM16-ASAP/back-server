package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ArticleRepository extends MongoRepository<Article, String> {
    
    // 제목 또는 작가로 키워드 검색
    @Query("{'$or': [{'title': {'$regex': ?0, '$options': 'i'}}, {'author': {'$regex': ?0, '$options': 'i'}}]}")
    Page<Article> findByTitleOrAuthorContaining(String keyword, Pageable pageable);
    
    // 태그와 키워드 모두 적용
    @Query("{'$and': [{'tags': {'$in': ?0}}, {'$or': [{'title': {'$regex': ?1, '$options': 'i'}}, {'author': {'$regex': ?1, '$options': 'i'}}]}]}")
    Page<Article> findByTagsInAndTitleOrAuthorContaining(List<String> tags, String keyword, Pageable pageable);
    
    // 태그 목록으로 필터링
    Page<Article> findByTagsIn(List<String> tags, Pageable pageable);
}