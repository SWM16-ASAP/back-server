package com.linglevel.api.content.book.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.repository.BookProgressRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
        testBook.setCreatedAt(LocalDateTime.now());

        when(bookService.existsById(anyString())).thenReturn(true);
        when(bookService.findById(anyString())).thenReturn(testBook);
        when(chunkRepository.countByChapterIdAndDifficultyLevel(anyString(), any(DifficultyLevel.class))).thenReturn(100L);
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
        progress.setUpdatedAt(LocalDateTime.now());

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
        progress.setUpdatedAt(LocalDateTime.now());

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
        progress.setUpdatedAt(LocalDateTime.now());

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