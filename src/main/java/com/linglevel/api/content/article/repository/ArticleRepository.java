package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, ArticleRepositoryCustom {

	default Optional<Article> findById(String id) {
		return findById(Long.valueOf(id));
	}

	default boolean existsById(String id) {
		return existsById(Long.valueOf(id));
	}

	@Modifying
	@Transactional
	@Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :id")
	void incrementViewCount(@Param("id") Long id);

	default void incrementViewCount(String id) {
		incrementViewCount(Long.valueOf(id));
	}

}
