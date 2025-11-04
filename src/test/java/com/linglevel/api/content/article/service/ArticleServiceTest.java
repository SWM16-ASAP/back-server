package com.linglevel.api.content.article.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.article.dto.ArticleResponse;
import com.linglevel.api.content.article.dto.GetArticlesRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleProgressRepository articleProgressRepository;

    @Mock
    private ArticleChunkRepository articleChunkRepository;

    @Mock
    private ArticleChunkService articleChunkService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ArticleService articleService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setDeleted(false);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - IN_PROGRESS")
    void testProgressFilterWithPagination_InProgress() {
        // Given: IN_PROGRESS 필터로 1페이지(limit=5) 요청
        GetArticlesRequest request = new GetArticlesRequest();
        request.setProgress(ProgressStatus.IN_PROGRESS);
        request.setPage(1);
        request.setLimit(5);

        // Mock data: 5개의 진행 중인 아티클
        List<Article> articles = createArticles(5, "Article", "Author", List.of("tag1"));

        // Mock Page 생성 (총 10개 중 5개)
        Page<Article> articlePage =
            new org.springframework.data.domain.PageImpl<>(articles, PageRequest.of(0, 5), 10);

        when(articleRepository.findArticlesWithFilters(any(), eq(testUser.getId()), any()))
            .thenReturn(articlePage);

        // Mock ArticleProgress for in-progress articles
        mockArticleProgress(articles, false);

        // When
        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getId());

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
        // Given: technology 태그로 필터링, 1페이지(limit=10)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setTags("technology");
        request.setPage(1);
        request.setLimit(10);

        // Mock data: 10개의 technology 태그 아티클
        List<Article> articles = createArticles(10, "Tech Article", "Author", List.of("technology"));

        Page<Article> articlePage =
            new org.springframework.data.domain.PageImpl<>(articles, PageRequest.of(0, 10), 15);

        when(articleRepository.findArticlesWithFilters(any(), eq(testUser.getId()), any()))
            .thenReturn(articlePage);

        // When
        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getId());

        // Then: 정확히 10개 반환, 총 15개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(15);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 아티클이 technology 태그를 가져야 함
        response.getData().forEach(article -> assertThat(article.getTags()).contains("technology"));
    }

    @Test
    @DisplayName("키워드 검색과 페이지네이션")
    void testKeywordSearchWithPagination() {
        // Given: "viking" 키워드로 검색, 1페이지(limit=10)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setKeyword("viking");
        request.setPage(1);
        request.setLimit(10);

        // Mock data: 10개의 "viking" 포함 아티클
        List<Article> articles = createArticles(10, "The Viking Story", "Author", List.of("tag1"));

        Page<Article> articlePage =
            new PageImpl<>(articles, PageRequest.of(0, 10), 12);

        when(articleRepository.findArticlesWithFilters(any(), eq(testUser.getId()), any()))
            .thenReturn(articlePage);

        // When
        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getId());

        // Then: 정확히 10개 반환, 총 12개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 아티클의 제목에 "viking"이 포함되어야 함
        response.getData().forEach(article -> assertThat(article.getTitle().toLowerCase()).contains("viking"));
    }

    @Test
    @DisplayName("복합 필터링 - 태그 + 진도 + 페이지네이션")
    void testCombinedFiltersWithPagination() {
        // Given: technology 태그 + IN_PROGRESS 필터, 1페이지(limit=5)
        GetArticlesRequest request = new GetArticlesRequest();
        request.setTags("technology");
        request.setProgress(ProgressStatus.IN_PROGRESS);
        request.setPage(1);
        request.setLimit(5);

        // Mock data: 5개의 technology 태그 + 진행중 아티클
        List<Article> articles = createArticles(5, "Tech Article", "Author", List.of("technology"));

        Page<Article> articlePage =
            new org.springframework.data.domain.PageImpl<>(articles, PageRequest.of(0, 5), 10);

        when(articleRepository.findArticlesWithFilters(any(), eq(testUser.getId()), any()))
            .thenReturn(articlePage);

        // Mock ArticleProgress for in-progress articles
        mockArticleProgress(articles, false);

        // When
        PageResponse<ArticleResponse> response = articleService.getArticles(request, testUser.getId());

        // Then: technology 태그 + 진행중인 아티클만 반환
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(2);

        response.getData().forEach(article -> {
            assertThat(article.getTags()).contains("technology");
            assertThat(article.getProgressPercentage()).isGreaterThan(0.0);
            assertThat(article.getIsCompleted()).isFalse();
        });
    }

    private List<Article> createArticles(int count, String titlePrefix, String authorPrefix, List<String> tags) {
        List<Article> articles = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            articles.add(createArticle(titlePrefix + " " + i, authorPrefix + " " + i, tags));
        }
        return articles;
    }

    private Article createArticle(String title, String author, List<String> tags) {
        Article article = new Article();
        article.setId("article-" + title.hashCode());
        article.setTitle(title);
        article.setAuthor(author);
        article.setTags(tags);
        article.setDifficultyLevel(DifficultyLevel.A1);
        article.setReadingTime(60);
        article.setAverageRating(4.5);
        article.setReviewCount(100);
        article.setViewCount(1000);
        article.setCreatedAt(Instant.now());
        return article;
    }

    private void mockArticleProgress(List<Article> articles, boolean isCompleted) {
        com.linglevel.api.content.article.entity.ArticleChunk mockChunk = new com.linglevel.api.content.article.entity.ArticleChunk();
        mockChunk.setId("test-chunk-id");
        mockChunk.setChunkNumber(50);
        when(articleChunkService.findById(anyString())).thenReturn(mockChunk);

        when(articleChunkRepository.countByArticleIdAndDifficultyLevel(anyString(), any(DifficultyLevel.class))).thenReturn(100L);

        for (Article article : articles) {
            ArticleProgress progress = createArticleProgress(testUser.getId(), article.getId(), isCompleted);
            when(articleProgressRepository.findByUserIdAndArticleId(testUser.getId(), article.getId()))
                .thenReturn(Optional.of(progress));
        }
    }

    private ArticleProgress createArticleProgress(String userId, String articleId, boolean isCompleted) {
        ArticleProgress progress = new ArticleProgress();
        progress.setUserId(userId);
        progress.setArticleId(articleId);
        progress.setChunkId("test-chunk-id");
        progress.setIsCompleted(isCompleted);
        progress.setUpdatedAt(Instant.now());
        return progress;
    }
}
