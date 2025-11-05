package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.streak.service.ReadingSessionService;
import com.linglevel.api.streak.service.StreakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgressService - 스트릭과 학습 완료 통합 테스트")
class ProgressServiceIntegrationTest {

    @Mock
    private BookService bookService;

    @Mock
    private ChapterService chapterService;

    @Mock
    private ChunkService chunkService;

    @Mock
    private BookProgressRepository bookProgressRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ProgressCalculationService progressCalculationService;

    @Mock
    private ReadingSessionService readingSessionService;

    @Mock
    private StreakService streakService;

    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private ProgressService progressService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_BOOK_ID = "book-123";
    private static final String TEST_CHAPTER_ID = "chapter-1";
    private static final String TEST_CHUNK_ID = "chunk-1";

    private Chapter testChapter;
    private Chunk testChunk;
    private BookProgress testProgress;

    @BeforeEach
    void setUp() {
        testChapter = new Chapter();
        testChapter.setId(TEST_CHAPTER_ID);
        testChapter.setBookId(TEST_BOOK_ID);
        testChapter.setChapterNumber(1);

        testChunk = new Chunk();
        testChunk.setId(TEST_CHUNK_ID);
        testChunk.setChapterId(TEST_CHAPTER_ID);
        testChunk.setChunkNumber(5); // 마지막 청크
        testChunk.setDifficultyLevel(DifficultyLevel.B1);

        testProgress = new BookProgress();
        testProgress.setUserId(TEST_USER_ID);
        testProgress.setBookId(TEST_BOOK_ID);
        testProgress.setChapterProgresses(new ArrayList<>());
    }

    @Test
    @DisplayName("챕터 완료 시 addCompletedContent와 updateStreak 모두 호출됨")
    void updateProgress_ChapterCompletion_CallsBothMethods() {
        // given
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(TEST_CHUNK_ID);

        when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
        when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
        when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
        when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
                .thenReturn(Optional.of(testProgress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1))
                .thenReturn(5L); // 마지막 청크 (5/5)
        when(chapterRepository.countByBookId(TEST_BOOK_ID))
                .thenReturn(10); // 총 10개 챕터
        when(readingSessionService.getReadingSessionSeconds(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID))
                .thenReturn(120L);

        // when
        progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

        // then - 세 가지 메서드가 순서대로 호출됨
        verify(streakService).addStudyTime(TEST_USER_ID, 120L);
        verify(streakService).updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID);
        verify(streakService).addCompletedContent(eq(TEST_USER_ID), eq(ContentType.BOOK), eq(TEST_CHAPTER_ID), anyBoolean());
    }

    @Test
    @DisplayName("같은 날 두 번째 챕터 완료 시 - 스트릭은 false, 완료 기록은 정상")
    void updateProgress_SecondChapterSameDay_OnlyCompletionRecorded() {
        // given
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(TEST_CHUNK_ID);

        when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
        when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
        when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
        when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
                .thenReturn(Optional.of(testProgress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1))
                .thenReturn(5L);
        when(chapterRepository.countByBookId(TEST_BOOK_ID))
                .thenReturn(10);
        when(readingSessionService.getReadingSessionSeconds(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID))
                .thenReturn(120L);

        // when
        progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

        // then - addCompletedContent는 여전히 호출됨
        verify(streakService).addStudyTime(TEST_USER_ID, 120L);
        verify(streakService).updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID);
        verify(streakService).addCompletedContent(eq(TEST_USER_ID), eq(ContentType.BOOK), eq(TEST_CHAPTER_ID), anyBoolean());
    }

    @Test
    @DisplayName("세션 유효하지 않아도 학습 시간과 완료 기록은 정상 처리")
    void updateProgress_InvalidSession_StudyTimeAndCompletionStillRecorded() {
        // given
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(TEST_CHUNK_ID);

        when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
        when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
        when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
        when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
                .thenReturn(Optional.of(testProgress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1))
                .thenReturn(5L);
        when(chapterRepository.countByBookId(TEST_BOOK_ID))
                .thenReturn(10);
        when(readingSessionService.getReadingSessionSeconds(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID))
                .thenReturn(30L); // 짧은 시간

        // when
        progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

        // then - 학습 시간과 완료 기록은 정상 처리됨
        verify(streakService).addStudyTime(TEST_USER_ID, 30L);
        verify(streakService).updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID);
        verify(streakService).addCompletedContent(eq(TEST_USER_ID), eq(ContentType.BOOK), eq(TEST_CHAPTER_ID), anyBoolean());
    }

    @Test
    @DisplayName("마지막 청크가 아니면 스트릭/완료 기록 메서드 호출 안됨")
    void updateProgress_NotLastChunk_NoStreakOrCompletionMethods() {
        // given
        testChunk.setChunkNumber(3); // 중간 청크
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setChunkId(TEST_CHUNK_ID);

        when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
        when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
        when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
        when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
                .thenReturn(Optional.of(testProgress));
        when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1))
                .thenReturn(5L); // 총 5개 중 3번째
        when(chapterRepository.countByBookId(TEST_BOOK_ID))
                .thenReturn(10);

        // when
        progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

        // then - 마지막 청크가 아니므로 세 메서드 모두 호출 안됨
        verify(streakService, never()).addStudyTime(any(), anyLong());
        verify(streakService, never()).addCompletedContent(any(), any(), any(), anyBoolean());
        verify(streakService, never()).updateStreak(any(), any(), any());
    }
}
