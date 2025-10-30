package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.streak.service.ReadingSessionService;
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
import static org.mockito.ArgumentMatchers.any;
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
    private ReadingSessionService readingSessionService;

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
}
