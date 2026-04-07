package com.linglevel.api.content.book.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.ChapterNavigationResponse;
import com.linglevel.api.content.book.dto.ChunkCountByLevelDto;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private BookProgressRepository bookProgressRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private BookService bookService;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ChapterService chapterService;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        testUser.setDeleted(false);
        testUser.setCreatedAt(LocalDateTime.now());

        testBook = new Book();
        testBook.setId("test-book-id");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setDifficultyLevel(DifficultyLevel.A1);
        testBook.setChapterCount(10);
        testBook.setCreatedAt(Instant.now());

        lenient().when(bookService.findById(anyString())).thenReturn(testBook);

        // Add stubs for the new repository methods called during refactoring
        lenient().when(chunkRepository.findChunkCountsByChapterIds(anyList())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - NOT_STARTED")
    void testProgressFilterWithPagination_NotStarted() {
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.NOT_STARTED)
                .page(1)
                .limit(3)
                .build();

        // Mock: 6-10번 챕터 (5번까지 읽어서 6번부터 NOT_STARTED)
        List<Chapter> chapters = createChapters(3, testBook.getId(), 6, "Chapter");

        Page<Chapter> chapterPage =
            new PageImpl<>(chapters, PageRequest.of(0, 3), 5);

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setChunkId("test-chunk-id");
        progress.setCurrentReadChapterNumber(5);
        progress.setMaxReadChapterNumber(5);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(Instant.now());

        com.linglevel.api.content.book.entity.Chunk mockChunk = new com.linglevel.api.content.book.entity.Chunk();
        mockChunk.setId("test-chunk-id");
        mockChunk.setChunkNumber(50);
        when(chunkRepository.findById(anyString())).thenReturn(Optional.of(mockChunk));

        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.of(progress));

        when(chapterRepository.findChaptersWithFilters(anyString(), any(), anyString(), any()))
            .thenReturn(chapterPage);

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getId());

        assertThat(response.getData()).hasSize(3);
        assertThat(response.getTotalCount()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - IN_PROGRESS")
    void testProgressFilterWithPagination_InProgress() {
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.IN_PROGRESS)
                .page(1)
                .limit(10)
                .build();

        // Mock: 5번 챕터만 IN_PROGRESS
        List<Chapter> chapters = createChapters(1, testBook.getId(), 5, "Chapter");

        Page<Chapter> chapterPage =
            new PageImpl<>(chapters, PageRequest.of(0, 10), 1);

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setChunkId("test-chunk-id");
        progress.setCurrentReadChapterNumber(5);
        progress.setMaxReadChapterNumber(5);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(Instant.now());

        com.linglevel.api.content.book.entity.Chunk mockChunk = new com.linglevel.api.content.book.entity.Chunk();
        mockChunk.setId("test-chunk-id");
        mockChunk.setChunkNumber(50);
        when(chunkRepository.findById(anyString())).thenReturn(Optional.of(mockChunk));

        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.of(progress));

        when(chapterRepository.findChaptersWithFilters(anyString(), any(), anyString(), any()))
            .thenReturn(chapterPage);

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getId());

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getData().get(0).getChapterNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("진도 필터링과 페이지네이션 - COMPLETED")
    void testProgressFilterWithPagination_Completed() {
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.COMPLETED)
                .page(1)
                .limit(2)
                .build();

        // Mock: 1-4번 챕터 (5번 진행중이므로 1-4번까지 완료)
        List<Chapter> chapters = createChapters(2, testBook.getId(), 1, "Chapter");

        Page<Chapter> chapterPage =
            new PageImpl<>(chapters, PageRequest.of(0, 2), 4);

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setChunkId("test-chunk-id");
        progress.setCurrentReadChapterNumber(5);
        progress.setMaxReadChapterNumber(5);
        progress.setIsCompleted(false);
        progress.setUpdatedAt(Instant.now());

        com.linglevel.api.content.book.entity.Chunk mockChunk = new com.linglevel.api.content.book.entity.Chunk();
        mockChunk.setId("test-chunk-id");
        mockChunk.setChunkNumber(50);
        when(chunkRepository.findById(anyString())).thenReturn(Optional.of(mockChunk));

        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.of(progress));

        when(chapterRepository.findChaptersWithFilters(anyString(), any(), anyString(), any()))
            .thenReturn(chapterPage);

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getId());

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getTotalCount()).isEqualTo(4);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("진도 정보 없을 때 - NOT_STARTED는 모든 챕터 반환")
    void testNoProgress_NotStarted() {
        GetChaptersRequest request = GetChaptersRequest.builder()
                .progress(ProgressStatus.NOT_STARTED)
                .page(1)
                .limit(5)
                .build();

        // Mock: 모든 챕터 (진도 정보 없음)
        List<Chapter> chapters = createChapters(5, testBook.getId(), 1, "Chapter");

        Page<Chapter> chapterPage =
            new PageImpl<>(chapters, PageRequest.of(0, 5), 10);

        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.empty());

        when(chapterRepository.findChaptersWithFilters(anyString(), any(), anyString(), any()))
            .thenReturn(chapterPage);

        PageResponse<ChapterResponse> response = chapterService.getChapters(testBook.getId(), request, testUser.getId());

        assertThat(response.getData()).hasSize(5);
        assertThat(response.getTotalCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("단일 챕터 조회 시 V3 chapterProgresses 정보를 기준으로 응답을 계산한다")
    void getChapter_usesV3ChapterProgressInfo() {
        // given
        Chapter chapter = createChapter(testBook.getId(), 2, "Chapter 2");

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setCurrentDifficultyLevel(DifficultyLevel.B1);
        progress.setChapterProgresses(List.of(
            BookProgress.ChapterProgressInfo.builder()
                .chapterNumber(2)
                .progressPercentage(37.5)
                .isCompleted(false)
                .build()
        ));

        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.of(progress));
        when(chunkRepository.findChunkCountsByChapterIds(List.of(chapter.getId())))
            .thenReturn(List.of(new ChunkCountByLevelDto(chapter.getId(), DifficultyLevel.B1, 8L)));

        // when
        ChapterResponse response = chapterService.getChapter(testBook.getId(), chapter.getId(), testUser.getId());

        // then
        assertThat(response.getId()).isEqualTo(chapter.getId());
        assertThat(response.getCurrentDifficultyLevel()).isEqualTo(DifficultyLevel.B1);
        assertThat(response.getChunkCount()).isEqualTo(8);
        assertThat(response.getProgressPercentage()).isEqualTo(37.5);
        assertThat(response.getCurrentReadChunkNumber()).isEqualTo(3);
        assertThat(response.getIsCompleted()).isFalse();
    }

    @Test
    @DisplayName("단일 챕터 조회 시 V3 데이터가 없으면 fallback progress 정보로 응답을 계산한다")
    void getChapter_usesFallbackProgressInfoWhenV3DataMissing() {
        // given
        Chapter chapter = createChapter(testBook.getId(), 2, "Chapter 2");

        BookProgress progress = new BookProgress();
        progress.setUserId(testUser.getId());
        progress.setBookId(testBook.getId());
        progress.setChunkId("progress-chunk");
        progress.setCurrentReadChapterNumber(2);
        progress.setCurrentDifficultyLevel(DifficultyLevel.B1);
        progress.setChapterProgresses(null);

        com.linglevel.api.content.book.entity.Chunk progressChunk = new com.linglevel.api.content.book.entity.Chunk();
        progressChunk.setId("progress-chunk");
        progressChunk.setChunkNumber(3);

        when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
        when(bookProgressRepository.findByUserIdAndBookId(testUser.getId(), testBook.getId()))
            .thenReturn(Optional.of(progress));
        when(chunkRepository.findById("progress-chunk")).thenReturn(Optional.of(progressChunk));
        when(chunkRepository.findChunkCountsByChapterIds(List.of(chapter.getId())))
            .thenReturn(List.of(new ChunkCountByLevelDto(chapter.getId(), DifficultyLevel.B1, 8L)));

        // when
        ChapterResponse response = chapterService.getChapter(testBook.getId(), chapter.getId(), testUser.getId());

        // then
        assertThat(response.getId()).isEqualTo(chapter.getId());
        assertThat(response.getCurrentDifficultyLevel()).isEqualTo(DifficultyLevel.B1);
        assertThat(response.getChunkCount()).isEqualTo(8);
        assertThat(response.getProgressPercentage()).isEqualTo(37.5);
        assertThat(response.getCurrentReadChunkNumber()).isEqualTo(3);
        assertThat(response.getIsCompleted()).isFalse();
    }

    @Test
    @DisplayName("챕터가 다른 책에 속하면 CHAPTER_NOT_FOUND_IN_BOOK 예외를 던진다")
    void getChapter_throwsWhenChapterDoesNotBelongToBook() {
        // given
        Chapter anotherBookChapter = createChapter("another-book", 1, "Wrong Chapter");
        when(chapterRepository.findById(anotherBookChapter.getId())).thenReturn(Optional.of(anotherBookChapter));

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> chapterService.getChapter(testBook.getId(), anotherBookChapter.getId(), testUser.getId())
        );

        // then
        assertThat(exception.getMessage()).isEqualTo(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK.getMessage());
    }

    @Test
    @DisplayName("챕터 네비게이션 조회 시 이전/다음 챕터 정보를 반환한다")
    void getChapterNavigation_returnsPreviousAndNextChapter() {
        // given
        Chapter currentChapter = createChapter(testBook.getId(), 2, "Chapter 2");
        Chapter previousChapter = createChapter(testBook.getId(), 1, "Chapter 1");
        Chapter nextChapter = createChapter(testBook.getId(), 3, "Chapter 3");

        when(bookService.existsById(testBook.getId())).thenReturn(true);
        when(chapterRepository.findById(currentChapter.getId())).thenReturn(Optional.of(currentChapter));
        when(chapterRepository.findByBookIdAndChapterNumber(testBook.getId(), 1)).thenReturn(Optional.of(previousChapter));
        when(chapterRepository.findByBookIdAndChapterNumber(testBook.getId(), 3)).thenReturn(Optional.of(nextChapter));

        // when
        ChapterNavigationResponse response = chapterService.getChapterNavigation(testBook.getId(), currentChapter.getId());

        // then
        assertThat(response.getCurrentChapterId()).isEqualTo(currentChapter.getId());
        assertThat(response.getCurrentChapterNumber()).isEqualTo(2);
        assertThat(response.getHasPreviousChapter()).isTrue();
        assertThat(response.getPreviousChapterId()).isEqualTo(previousChapter.getId());
        assertThat(response.getHasNextChapter()).isTrue();
        assertThat(response.getNextChapterId()).isEqualTo(nextChapter.getId());
    }

    @Test
    @DisplayName("챕터 목록 조회 시 viewCount를 증가시킨다")
    void getChapters_incrementsBookViewCount() {
        // given
        GetChaptersRequest request = GetChaptersRequest.builder()
            .page(1)
            .limit(2)
            .build();

        List<Chapter> chapters = createChapters(1, testBook.getId(), 1, "Chapter");
        Page<Chapter> chapterPage = new PageImpl<>(chapters, PageRequest.of(0, 2), 1);

        when(chapterRepository.findChaptersWithFilters(anyString(), any(), any(), any()))
            .thenReturn(chapterPage);

        // when
        chapterService.getChapters(testBook.getId(), request, testUser.getId());

        // then
        verify(bookRepository).incrementViewCount(testBook.getId());
    }

    private List<Chapter> createChapters(int count, String bookId, int startNumber, String titlePrefix) {
        List<Chapter> chapters = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            int chapterNum = startNumber + i;
            chapters.add(createChapter(bookId, chapterNum, titlePrefix + " " + chapterNum));
        }
        return chapters;
    }

    private Chapter createChapter(String bookId, Integer chapterNumber, String title) {
        Chapter chapter = new Chapter();
        chapter.setId("chapter-" + chapterNumber);
        chapter.setBookId(bookId);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(title);
        chapter.setReadingTime(30);
        return chapter;
    }
}
