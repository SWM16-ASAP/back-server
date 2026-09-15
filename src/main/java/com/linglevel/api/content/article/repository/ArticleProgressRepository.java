package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.ArticleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface ArticleProgressRepository extends JpaRepository<ArticleProgress, Long> {

	Optional<ArticleProgress> findByUserIdAndArticleId(Long userId, Long articleId);

	default Optional<ArticleProgress> findByUserIdAndArticleId(String userId, String articleId) {
		return findByUserIdAndArticleId(Long.valueOf(userId), Long.valueOf(articleId));
	}

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from ArticleProgress p where p.userId = :userId and p.articleId = :contentId")
	Optional<ArticleProgress> findForUpdate(Long userId, Long contentId);

	default Optional<ArticleProgress> findForUpdate(String userId, String contentId) {
		return findForUpdate(Long.valueOf(userId), Long.valueOf(contentId));
	}

	List<ArticleProgress> findAllByUserId(Long userId);

	default List<ArticleProgress> findAllByUserId(String userId) {
		return findAllByUserId(Long.valueOf(userId));
	}

}
