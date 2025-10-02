package com.linglevel.api.content.book.service;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChapterServiceTest extends AbstractDatabaseTest {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookProgressRepository bookProgressRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        bookProgressRepository.deleteAll();
        chapterRepository.deleteAll();
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

        // 테스트 책 생성
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setDifficultyLevel(DifficultyLevel.A1);
        testBook.setChapterCount(10);
        testBook.setCreatedAt(LocalDateTime.now());
        testBook = bookRepository.save(testBook);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - NOT_STARTED")
    void testProgressFilterWithPagination_NotStarted() {
        // Given: 10개 챕터 생성, 현재 5번 챕터까지 읽음
        for (int i = 1; i <= 10; i++) {
            Chapter chapter = createChapter(testBook.getId(), i, "Chapter " + i);
            chapterRepository.save(chapter);
        }

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setCurrentReadChapterNumber(5);
        progress.setCurrentReadChunkNumber(50);
        progress.setMaxReadChapterNumber(5);
        progress.setMaxReadChunkNumber(50);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(LocalDateTime.now());
        bookProgressRepository.save(progress);

        // When: NOT_STARTED 필터로 1페이지(limit=3) 요청
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.NOT_STARTED)
                .page(1)
                .limit(3)
                .build();

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getUsername());

        // Then: 6-10번 챕터 중 첫 3개 반환
        assertThat(response.getData()).hasSize(3);
        assertThat(response.getTotalCount()).isEqualTo(5); // 6-10번 = 5개
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - IN_PROGRESS")
    void testProgressFilterWithPagination_InProgress() {
        // Given: 10개 챕터 생성, 현재 5번 챕터 읽는 중
        for (int i = 1; i <= 10; i++) {
            Chapter chapter = createChapter(testBook.getId(), i, "Chapter " + i);
            chapterRepository.save(chapter);
        }

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setCurrentReadChapterNumber(5);
        progress.setCurrentReadChunkNumber(50);
        progress.setMaxReadChapterNumber(5);
        progress.setMaxReadChunkNumber(50);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(LocalDateTime.now());
        bookProgressRepository.save(progress);

        // When: IN_PROGRESS 필터로 요청
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.IN_PROGRESS)
                .page(1)
                .limit(10)
                .build();

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getUsername());

        // Then: 5번 챕터만 반환
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getData().get(0).getChapterNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - COMPLETED")
    void testProgressFilterWithPagination_Completed() {
        // Given: 10개 챕터 생성, 현재 5번 챕터까지 읽음
        for (int i = 1; i <= 10; i++) {
            Chapter chapter = createChapter(testBook.getId(), i, "Chapter " + i);
            chapterRepository.save(chapter);
        }

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setCurrentReadChapterNumber(5);
        progress.setCurrentReadChunkNumber(50);
        progress.setMaxReadChapterNumber(5);
        progress.setMaxReadChunkNumber(50);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(LocalDateTime.now());
        bookProgressRepository.save(progress);

        // When: COMPLETED 필터로 1페이지(limit=2) 요청
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.COMPLETED)
                .page(1)
                .limit(2)
                .build();

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getUsername());

        // Then: 1-4번 챕터 중 첫 2개 반환
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getTotalCount()).isEqualTo(4); // 1-4번 = 4개
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("진도 정보 없을 때 - NOT_STARTED는 모든 챕터 반환")
    void testNoProgress_NotStarted() {
        // Given: 10개 챕터 생성, 진도 정보 없음
        for (int i = 1; i <= 10; i++) {
            Chapter chapter = createChapter(testBook.getId(), i, "Chapter " + i);
            chapterRepository.save(chapter);
        }

        // When: NOT_STARTED 필터로 요청
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.NOT_STARTED)
                .page(1)
                .limit(5)
                .build();

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getUsername());

        // Then: 모든 챕터가 NOT_STARTED이므로 첫 5개 반환
        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10);
    }

    private Chapter createChapter(String bookId, Integer chapterNumber, String title) {
        Chapter chapter = new Chapter();
        chapter.setBookId(bookId);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(title);
        chapter.setChunkCount(100);
        chapter.setReadingTime(30);
        return chapter;
    }
}
