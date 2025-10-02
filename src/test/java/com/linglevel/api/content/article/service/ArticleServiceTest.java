package com.linglevel.api.content.article.service;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.article.dto.ArticleResponse;
import com.linglevel.api.content.article.dto.GetArticlesRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ArticleServiceTest extends AbstractDatabaseTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleProgressRepository articleProgressRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        articleProgressRepository.deleteAll();
        articleRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 유저 생성
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setDeleted(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - IN_PROGRESS")
    void testProgressFilterWithPagination_InProgress() {
        // Given: 30개 아티클 생성
        for (int i = 1; i <= 30; i++) {
            Article article = createArticle("Article " + i, "Author " + i, Arrays.asList("tag1"));
            articleRepository.save(article);

            // 10개는 진행중, 10개는 완료, 10개는 시작안함
            if (i <= 10) {
                createArticleProgress(testUser.getId(), article.getId(), false);
            } else if (i <= 20) {
                createArticleProgress(testUser.getId(), article.getId(), true);
            }
        }

        // When: IN_PROGRESS 필터로 1페이지(limit=5) 요청
        GetArticlesRequest request = new GetArticlesRequest();
        request.setProgress(ProgressStatus.IN_PROGRESS);
        request.setPage(1);
        request.setLimit(5);

        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getUsername());

        // Then: 정확히 5개 반환, 총 10개
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 항목이 진행중이어야 함
        response.getData().forEach(article -> {
            assertThat(article.getProgressPercentage()).isGreaterThan(0.0);
            assertThat(article.getIsCompleted()).isFalse();
        });
    }

    @Test
    @DisplayName("태그 필터링과 페이지네이션")
    void testTagsFilterWithPagination() {
        // Given: technology 태그 15개, business 태그 15개
        for (int i = 1; i <= 15; i++) {
            Article article = createArticle("Tech Article " + i, "Author " + i, Arrays.asList("technology"));
            articleRepository.save(article);
        }
        for (int i = 1; i <= 15; i++) {
            Article article = createArticle("Business Article " + i, "Author " + i, Arrays.asList("business"));
            articleRepository.save(article);
        }

        // When: technology 태그로 필터링, 1페이지(limit=10)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setTags("technology");
        request.setPage(1);
        request.setLimit(10);

        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getUsername());

        // Then: 정확히 10개 반환, 총 15개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(15);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 아티클이 technology 태그를 가져야 함
        response.getData().forEach(article -> {
            assertThat(article.getTags()).contains("technology");
        });
    }

    @Test
    @DisplayName("키워드 검색과 페이지네이션")
    void testKeywordSearchWithPagination() {
        // Given: 제목에 "viking" 포함된 아티클 12개, 다른 아티클 18개
        for (int i = 1; i <= 12; i++) {
            Article article = createArticle("The Viking Story " + i, "Author " + i, Arrays.asList("tag1"));
            articleRepository.save(article);
        }
        for (int i = 1; i <= 18; i++) {
            Article article = createArticle("Other Article " + i, "Author " + i, Arrays.asList("tag1"));
            articleRepository.save(article);
        }

        // When: "viking" 키워드로 검색, 1페이지(limit=10)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setKeyword("viking");
        request.setPage(1);
        request.setLimit(10);

        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getUsername());

        // Then: 정확히 10개 반환, 총 12개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 아티클의 제목에 "viking"이 포함되어야 함
        response.getData().forEach(article -> {
            assertThat(article.getTitle().toLowerCase()).contains("viking");
        });
    }

    @Test
    @DisplayName("복합 필터링 - 태그 + 진도 + 페이지네이션")
    void testCombinedFiltersWithPagination() {
        // Given: technology 태그를 가진 아티클 20개 생성
        for (int i = 1; i <= 20; i++) {
            Article article = createArticle("Tech Article " + i, "Author " + i, Arrays.asList("technology"));
            articleRepository.save(article);

            // 절반만 진행중으로 설정
            if (i <= 10) {
                createArticleProgress(testUser.getId(), article.getId(), false);
            }
        }

        // business 태그를 가진 아티클 10개 생성 (진행중)
        for (int i = 1; i <= 10; i++) {
            Article article = createArticle("Business Article " + i, "Author " + i, Arrays.asList("business"));
            articleRepository.save(article);
            createArticleProgress(testUser.getId(), article.getId(), false);
        }

        // When: technology 태그 + IN_PROGRESS 필터, 1페이지(limit=5)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setTags("technology");
        request.setProgress(ProgressStatus.IN_PROGRESS);
        request.setPage(1);
        request.setLimit(5);

        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getUsername());

        // Then: technology 태그 + 진행중인 아티클만 반환
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10); // technology + 진행중 = 10개
        assertThat(response.getTotalPages()).isEqualTo(2);

        response.getData().forEach(article -> {
            assertThat(article.getTags()).contains("technology");
            assertThat(article.getProgressPercentage()).isGreaterThan(0.0);
            assertThat(article.getIsCompleted()).isFalse();
        });
    }

    private Article createArticle(String title, String author, List<String> tags) {
        Article article = new Article();
        article.setTitle(title);
        article.setAuthor(author);
        article.setTags(tags);
        article.setDifficultyLevel(DifficultyLevel.A1);
        article.setChunkCount(100);
        article.setReadingTime(60);
        article.setAverageRating(4.5);
        article.setReviewCount(100);
        article.setViewCount(1000);
        article.setCreatedAt(LocalDateTime.now());
        return article;
    }

    private void createArticleProgress(String userId, String articleId, boolean isCompleted) {
        ArticleProgress progress = new ArticleProgress();
        progress.setUserId(userId);
        progress.setArticleId(articleId);
        progress.setCurrentReadChunkNumber(isCompleted ? 100 : 50);
        progress.setMaxReadChunkNumber(isCompleted ? 100 : 50);
        progress.setIsCompleted(isCompleted);
        progress.setUpdatedAt(LocalDateTime.now());
        articleProgressRepository.save(progress);
    }
}
