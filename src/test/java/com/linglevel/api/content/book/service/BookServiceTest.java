package com.linglevel.api.content.book.service;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.BookResponse;
import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
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
class BookServiceTest extends AbstractDatabaseTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookProgressRepository bookProgressRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        bookProgressRepository.deleteAll();
        bookRepository.deleteAll();
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
    @DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - IN_PROGRESS 필터")
    void testProgressFilterWithPagination_InProgress() {
        // Given: 30개의 책 생성
        for (int i = 1; i <= 30; i++) {
            Book book = createBook("Book " + i, "Author " + i, Arrays.asList("tag1"));
            bookRepository.save(book);

            // 10개는 진행중, 10개는 완료, 10개는 시작안함
            if (i <= 10) {
                // 진행중
                createBookProgress(testUser.getId(), book.getId(), false);
            } else if (i <= 20) {
                // 완료
                createBookProgress(testUser.getId(), book.getId(), true);
            }
            // 21~30은 진행 정보 없음 (NOT_STARTED)
        }

        // When: IN_PROGRESS 필터로 1페이지(limit=5) 요청
        GetBooksRequest request = GetBooksRequest.builder()
                .progress(ProgressStatus.IN_PROGRESS)
                .page(1)
                .limit(5)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: 정확히 5개가 반환되어야 함 (진행중 10개 중 첫 5개)
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10); // 진행중 총 10개
        assertThat(response.getTotalPages()).isEqualTo(2); // 10개 / 5 = 2페이지

        // 모든 항목이 진행중이어야 함
        response.getData().forEach(book -> {
            assertThat(book.getProgressPercentage()).isGreaterThan(0.0);
            assertThat(book.getIsCompleted()).isFalse();
        });
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - COMPLETED 필터")
    void testProgressFilterWithPagination_Completed() {
        // Given: 30개의 책 생성
        for (int i = 1; i <= 30; i++) {
            Book book = createBook("Book " + i, "Author " + i, Arrays.asList("tag1"));
            bookRepository.save(book);

            if (i <= 10) {
                createBookProgress(testUser.getId(), book.getId(), false);
            } else if (i <= 20) {
                createBookProgress(testUser.getId(), book.getId(), true);
            }
        }

        // When: COMPLETED 필터로 1페이지(limit=5) 요청
        GetBooksRequest request = GetBooksRequest.builder()
                .progress(ProgressStatus.COMPLETED)
                .page(1)
                .limit(5)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: 정확히 5개가 반환되어야 함 (완료 10개 중 첫 5개)
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10); // 완료 총 10개
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 항목이 완료되어야 함
        response.getData().forEach(book -> {
            assertThat(book.getIsCompleted()).isTrue();
        });
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - NOT_STARTED 필터")
    void testProgressFilterWithPagination_NotStarted() {
        // Given: 30개의 책 생성
        for (int i = 1; i <= 30; i++) {
            Book book = createBook("Book " + i, "Author " + i, Arrays.asList("tag1"));
            bookRepository.save(book);

            if (i <= 10) {
                createBookProgress(testUser.getId(), book.getId(), false);
            } else if (i <= 20) {
                createBookProgress(testUser.getId(), book.getId(), true);
            }
        }

        // When: NOT_STARTED 필터로 1페이지(limit=5) 요청
        GetBooksRequest request = GetBooksRequest.builder()
                .progress(ProgressStatus.NOT_STARTED)
                .page(1)
                .limit(5)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: 정확히 5개가 반환되어야 함 (시작안함 10개 중 첫 5개)
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10); // 시작안함 총 10개
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 항목이 시작하지 않았어야 함
        response.getData().forEach(book -> {
            assertThat(book.getProgressPercentage()).isEqualTo(0.0);
        });
    }

    @Test
    @DisplayName("태그 필터링과 페이지네이션")
    void testTagsFilterWithPagination() {
        // Given: philosophy 태그를 가진 책 15개, children 태그를 가진 책 15개
        for (int i = 1; i <= 15; i++) {
            Book book = createBook("Philosophy Book " + i, "Author " + i, Arrays.asList("philosophy"));
            bookRepository.save(book);
        }
        for (int i = 1; i <= 15; i++) {
            Book book = createBook("Children Book " + i, "Author " + i, Arrays.asList("children"));
            bookRepository.save(book);
        }

        // When: philosophy 태그로 필터링, 1페이지(limit=10)
        GetBooksRequest request = GetBooksRequest.builder()
                .tags("philosophy")
                .page(1)
                .limit(10)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: 정확히 10개 반환, 총 15개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(15);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 책이 philosophy 태그를 가져야 함
        response.getData().forEach(book -> {
            assertThat(book.getTags()).contains("philosophy");
        });
    }

    @Test
    @DisplayName("키워드 검색과 페이지네이션")
    void testKeywordSearchWithPagination() {
        // Given: 제목에 "prince"가 포함된 책 12개, 다른 책 18개
        for (int i = 1; i <= 12; i++) {
            Book book = createBook("The Little Prince " + i, "Author " + i, Arrays.asList("tag1"));
            bookRepository.save(book);
        }
        for (int i = 1; i <= 18; i++) {
            Book book = createBook("Other Book " + i, "Author " + i, Arrays.asList("tag1"));
            bookRepository.save(book);
        }

        // When: "prince" 키워드로 검색, 1페이지(limit=10)
        GetBooksRequest request = GetBooksRequest.builder()
                .keyword("prince")
                .page(1)
                .limit(10)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: 정확히 10개 반환, 총 12개
        assertThat(response.getData()).hasSize(10);
        assertThat(response.getTotalCount()).isEqualTo(12);
        assertThat(response.getTotalPages()).isEqualTo(2);

        // 모든 책의 제목에 "prince"가 포함되어야 함
        response.getData().forEach(book -> {
            assertThat(book.getTitle().toLowerCase()).contains("prince");
        });
    }

    @Test
    @DisplayName("복합 필터링 - 태그 + 진도 + 페이지네이션")
    void testCombinedFiltersWithPagination() {
        // Given: philosophy 태그를 가진 책 20개 생성
        for (int i = 1; i <= 20; i++) {
            Book book = createBook("Philosophy Book " + i, "Author " + i, Arrays.asList("philosophy"));
            bookRepository.save(book);

            // 절반만 진행중으로 설정
            if (i <= 10) {
                createBookProgress(testUser.getId(), book.getId(), false);
            }
        }

        // children 태그를 가진 책 10개 생성 (진행중)
        for (int i = 1; i <= 10; i++) {
            Book book = createBook("Children Book " + i, "Author " + i, Arrays.asList("children"));
            bookRepository.save(book);
            createBookProgress(testUser.getId(), book.getId(), false);
        }

        // When: philosophy 태그 + IN_PROGRESS 필터, 1페이지(limit=5)
        GetBooksRequest request = GetBooksRequest.builder()
                .tags("philosophy")
                .progress(ProgressStatus.IN_PROGRESS)
                .page(1)
                .limit(5)
                .build();

        PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getUsername());

        // Then: philosophy 태그 + 진행중인 책만 반환
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10); // philosophy + 진행중 = 10개
        assertThat(response.getTotalPages()).isEqualTo(2);

        response.getData().forEach(book -> {
            assertThat(book.getTags()).contains("philosophy");
            assertThat(book.getProgressPercentage()).isGreaterThan(0.0);
            assertThat(book.getIsCompleted()).isFalse();
        });
    }

    private Book createBook(String title, String author, List<String> tags) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setTags(tags);
        book.setDifficultyLevel(DifficultyLevel.A1);
        book.setChapterCount(10);
        book.setReadingTime(60);
        book.setAverageRating(4.5);
        book.setReviewCount(100);
        book.setViewCount(1000);
        book.setCreatedAt(LocalDateTime.now());
        return book;
    }

    private void createBookProgress(String userId, String bookId, boolean isCompleted) {
        BookProgress progress = new BookProgress();
        progress.setUserId(userId);
        progress.setBookId(bookId);
        progress.setCurrentReadChapterNumber(isCompleted ? 10 : 5);
        progress.setCurrentReadChunkNumber(isCompleted ? 100 : 50);
        progress.setMaxReadChapterNumber(isCompleted ? 10 : 5);
        progress.setMaxReadChunkNumber(isCompleted ? 100 : 50);
        progress.setIsCompleted(isCompleted);
        progress.setUpdatedAt(LocalDateTime.now());
        bookProgressRepository.save(progress);
    }
}
