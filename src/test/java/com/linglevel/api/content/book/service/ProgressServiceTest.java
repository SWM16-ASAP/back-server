package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.content.common.service.ReadingCompletionService;
import com.linglevel.api.streak.service.StreakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private BookProgressRepository bookProgressRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ChapterService chapterService;

    @Mock
    private ChunkService chunkService;

    @Mock
    private ProgressCalculationService progressCalculationService;

    @Mock
    private ReadingCompletionService readingCompletionService;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private ProgressService progressService;

    @Captor
    private ArgumentCaptor<BookProgress> bookProgressCaptor;

    @Test
    @DisplayName("오래된 Book 진행률 업데이트 시 V3 필드(chapterProgresses)가 정상적으로 마이그레이션된다")
    void updateProgress_shouldLazyMigrate_forOldBookProgress() {
        // Given: 마이그레이션되지 않은(V3 필드가 null인) BookProgress 설정
        String userId = "test-user";
        String bookId = "test-book";
        String chunkId = "test-chunk";
        String chapterId = "test-chapter";

        // V3 필드(chapterProgresses)가 null인 레거시 데이터
        BookProgress legacyProgress = new BookProgress();
        legacyProgress.setId("legacy-progress-id");
        legacyProgress.setUserId(userId);
        legacyProgress.setBookId(bookId);
        legacyProgress.setChapterProgresses(null); // This is the legacy state

        Chunk currentChunk = new Chunk();
        currentChunk.setId(chunkId);
        currentChunk.setChapterId(chapterId);
        currentChunk.setChunkNumber(1);

        Chapter currentChapter = new Chapter();
        currentChapter.setId(chapterId);
        currentChapter.setBookId(bookId);
        currentChapter.setChapterNumber(1);

        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(chunkId);

        // Mocking
        when(bookService.existsById(bookId)).thenReturn(true);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(legacyProgress));
        when(chunkService.findById(chunkId)).thenReturn(currentChunk);
        // The service first finds the chunk, then gets the chapterId from it, then finds the chapter.
        when(chapterService.findById(chapterId)).thenReturn(currentChapter);
        when(chapterRepository.countByBookId(bookId)).thenReturn(10);
        when(chunkRepository.countByChapterIdAndDifficultyLevel(any(), any())).thenReturn(100L);

        // When: 진행률 업데이트 호출
        progressService.updateProgress(bookId, request, userId);

        // Then: V3 필드(chapterProgresses)가 채워진 상태로 저장되는지 검증
        verify(bookProgressRepository).save(bookProgressCaptor.capture());
        BookProgress savedProgress = bookProgressCaptor.getValue();

        assertThat(savedProgress.getId()).isEqualTo("legacy-progress-id");
        assertThat(savedProgress.getChapterProgresses()).isNotNull();
        // ensureMigrated initializes the list, and the subsequent logic adds the first progress info
        assertThat(savedProgress.getChapterProgresses()).hasSize(1);
        assertThat(savedProgress.getChapterProgresses().get(0).getChapterNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("진도 정보가 없으면 첫 챕터와 첫 청크 기준으로 초기 진도를 생성해 반환한다")
    void getProgress_initializesProgressWhenMissing() {
        // given
        String userId = "user-1";
        String bookId = "book-1";

        Chapter firstChapter = new Chapter();
        firstChapter.setId("chapter-1");
        firstChapter.setChapterNumber(1);

        Chunk firstChunk = new Chunk();
        firstChunk.setId("chunk-1");
        firstChunk.setChapterId("chapter-1");
        firstChunk.setChunkNumber(1);
        firstChunk.setDifficultyLevel(DifficultyLevel.A1);

        BookProgress savedProgress = new BookProgress();
        savedProgress.setId("progress-1");
        savedProgress.setUserId(userId);
        savedProgress.setBookId(bookId);
        savedProgress.setChapterId("chapter-1");
        savedProgress.setChunkId("chunk-1");
        savedProgress.setCurrentReadChapterNumber(1);
        savedProgress.setMaxReadChapterNumber(1);
        savedProgress.setCurrentDifficultyLevel(DifficultyLevel.A1);
        savedProgress.setNormalizedProgress(12.5);
        savedProgress.setMaxNormalizedProgress(12.5);

        when(bookService.existsById(bookId)).thenReturn(true);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());
        when(chapterService.findFirstByBookId(bookId)).thenReturn(firstChapter);
        when(chunkService.findFirstByChapterId("chapter-1")).thenReturn(firstChunk);
        when(chunkRepository.countByChapterIdAndDifficultyLevel("chapter-1", DifficultyLevel.A1)).thenReturn(8L);
        when(progressCalculationService.calculateNormalizedProgress(1, 8L)).thenReturn(12.5);
        when(bookProgressRepository.save(any(BookProgress.class))).thenReturn(savedProgress);
        when(chunkService.findById("chunk-1")).thenReturn(firstChunk);

        // when
        ProgressResponse response = progressService.getProgress(bookId, userId);

        // then
        verify(bookProgressRepository).save(bookProgressCaptor.capture());
        BookProgress initialized = bookProgressCaptor.getValue();

        assertThat(initialized.getUserId()).isEqualTo(userId);
        assertThat(initialized.getBookId()).isEqualTo(bookId);
        assertThat(initialized.getChapterId()).isEqualTo("chapter-1");
        assertThat(initialized.getChunkId()).isEqualTo("chunk-1");
        assertThat(initialized.getCurrentReadChapterNumber()).isEqualTo(1);
        assertThat(initialized.getMaxReadChapterNumber()).isEqualTo(1);
        assertThat(initialized.getCurrentDifficultyLevel()).isEqualTo(DifficultyLevel.A1);

        assertThat(response.getId()).isEqualTo("progress-1");
        assertThat(response.getCurrentReadChunkNumber()).isEqualTo(1);
        assertThat(response.getNormalizedProgress()).isEqualTo(12.5);
        assertThat(response.getStreakUpdated()).isFalse();
    }

    @Test
    @DisplayName("기존 챕터 진행률이 있으면 같은 챕터 항목을 업데이트하고 중복 추가하지 않는다")
    void updateProgress_updatesExistingChapterProgressEntry() {
        // given
        String userId = "user-1";
        String bookId = "book-1";
        String chunkId = "chunk-3";
        String chapterId = "chapter-1";

        BookProgress progress = new BookProgress();
        progress.setId("progress-1");
        progress.setUserId(userId);
        progress.setBookId(bookId);
        progress.setChapterProgresses(new ArrayList<>());
        progress.getChapterProgresses().add(BookProgress.ChapterProgressInfo.builder()
            .chapterNumber(1)
            .progressPercentage(20.0)
            .isCompleted(false)
            .build());

        Chunk chunk = new Chunk();
        chunk.setId(chunkId);
        chunk.setChapterId(chapterId);
        chunk.setChunkNumber(3);
        chunk.setDifficultyLevel(DifficultyLevel.A1);

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setBookId(bookId);
        chapter.setChapterNumber(1);

        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(chunkId);

        when(bookService.existsById(bookId)).thenReturn(true);
        when(chunkService.findById(chunkId)).thenReturn(chunk);
        when(chapterService.findById(chapterId)).thenReturn(chapter);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(progress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(chapterId, DifficultyLevel.A1)).thenReturn(5L);
        when(chapterRepository.countByBookId(bookId)).thenReturn(10);
        when(readingCompletionService.processReadingCompletion(userId, com.linglevel.api.content.common.ContentType.BOOK, chapterId, null))
            .thenReturn(null);

        // when
        ProgressResponse response = progressService.updateProgress(bookId, request, userId);

        // then
        verify(bookProgressRepository).save(bookProgressCaptor.capture());
        BookProgress saved = bookProgressCaptor.getValue();

        assertThat(saved.getChapterProgresses()).hasSize(1);
        assertThat(saved.getChapterProgresses().get(0).getChapterNumber()).isEqualTo(1);
        assertThat(saved.getChapterProgresses().get(0).getProgressPercentage()).isEqualTo(60.0);
        assertThat(saved.getChapterProgresses().get(0).getIsCompleted()).isFalse();
        assertThat(saved.getCurrentReadChapterNumber()).isEqualTo(1);
        assertThat(saved.getChunkId()).isEqualTo(chunkId);
        assertThat(response.getCurrentReadChunkNumber()).isEqualTo(3);
        assertThat(response.getStreakUpdated()).isFalse();
    }

    @Test
    @DisplayName("마지막 남은 챕터를 완료하면 책 전체를 완료 상태로 저장하고 streakUpdated를 반영한다")
    void updateProgress_marksBookCompletedWhenLastRemainingChapterFinishes() {
        // given
        String userId = "user-1";
        String bookId = "book-1";
        String chunkId = "chunk-4";
        String chapterId = "chapter-2";

        BookProgress progress = new BookProgress();
        progress.setId("progress-1");
        progress.setUserId(userId);
        progress.setBookId(bookId);
        progress.setIsCompleted(false);
        progress.setChapterProgresses(new ArrayList<>());
        progress.getChapterProgresses().add(BookProgress.ChapterProgressInfo.builder()
            .chapterNumber(1)
            .progressPercentage(100.0)
            .isCompleted(true)
            .build());

        Chunk chunk = new Chunk();
        chunk.setId(chunkId);
        chunk.setChapterId(chapterId);
        chunk.setChunkNumber(4);
        chunk.setDifficultyLevel(DifficultyLevel.A1);

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setBookId(bookId);
        chapter.setChapterNumber(2);

        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(chunkId);

        when(bookService.existsById(bookId)).thenReturn(true);
        when(chunkService.findById(chunkId)).thenReturn(chunk);
        when(chapterService.findById(chapterId)).thenReturn(chapter);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(progress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(chapterId, DifficultyLevel.A1)).thenReturn(4L);
        when(chapterRepository.countByBookId(bookId)).thenReturn(2);
        when(readingCompletionService.processReadingCompletion(userId, com.linglevel.api.content.common.ContentType.BOOK, chapterId, null))
            .thenReturn(45L);
        when(streakService.updateStreak(userId, com.linglevel.api.content.common.ContentType.BOOK, chapterId))
            .thenReturn(true);

        // when
        ProgressResponse response = progressService.updateProgress(bookId, request, userId);

        // then
        verify(bookProgressRepository).save(bookProgressCaptor.capture());
        BookProgress saved = bookProgressCaptor.getValue();

        assertThat(saved.getChapterProgresses()).hasSize(2);
        assertThat(saved.getChapterProgresses().get(1).getChapterNumber()).isEqualTo(2);
        assertThat(saved.getChapterProgresses().get(1).getProgressPercentage()).isEqualTo(100.0);
        assertThat(saved.getChapterProgresses().get(1).getIsCompleted()).isTrue();
        assertThat(saved.getIsCompleted()).isTrue();
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(response.getCurrentReadChunkNumber()).isEqualTo(4);
        assertThat(response.getStreakUpdated()).isTrue();
    }

    @Test
    @DisplayName("deleteProgress는 기존 진도 정보를 삭제한다")
    void deleteProgress_deletesExistingProgress() {
        // given
        String userId = "user-1";
        String bookId = "book-1";

        BookProgress progress = new BookProgress();
        progress.setId("progress-1");

        when(bookService.existsById(bookId)).thenReturn(true);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(progress));

        // when
        progressService.deleteProgress(bookId, userId);

        // then
        verify(bookProgressRepository).delete(progress);
    }

    @Test
    @DisplayName("deleteProgress는 진도 정보가 없으면 PROGRESS_NOT_FOUND 예외를 던진다")
    void deleteProgress_throwsWhenProgressMissing() {
        // given
        String userId = "user-1";
        String bookId = "book-1";

        when(bookService.existsById(bookId)).thenReturn(true);
        when(bookProgressRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> progressService.deleteProgress(bookId, userId)
        );

        // then
        assertThat(exception.getMessage()).isEqualTo(BooksErrorCode.PROGRESS_NOT_FOUND.getMessage());
        verify(bookProgressRepository, never()).delete(any(BookProgress.class));
    }
}
