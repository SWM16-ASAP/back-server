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
import com.linglevel.api.content.common.service.ReadingCompletionService;
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
	private ReadingCompletionService readingCompletionService;

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
		when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1)).thenReturn(5L); // 마지막
																														// 청크
																														// (5/5)
		when(chapterRepository.countByBookId(TEST_BOOK_ID)).thenReturn(10); // 총 10개 챕터
		when(readingCompletionService.processReadingCompletion(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, null))
			.thenReturn(120L);
		when(streakService.updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID)).thenReturn(true);

		// when
		progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

		// then - 세 가지 메서드가 순서대로 호출됨
		verify(streakService).addStudyTime(TEST_USER_ID, 120L);
		verify(streakService).updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID);
		verify(streakService).addCompletedContent(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, true);
	}

	@Test
	@DisplayName("updateStreak가 false를 반환하면 완료 기록에도 false를 전달한다")
	void updateProgress_passesFalseWhenStreakIsNotUpdated() {
		// given
		ProgressUpdateRequest request = new ProgressUpdateRequest();
		request.setChunkId(TEST_CHUNK_ID);

		when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
		when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
		when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
		when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
			.thenReturn(Optional.of(testProgress));
		when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1)).thenReturn(5L);
		when(chapterRepository.countByBookId(TEST_BOOK_ID)).thenReturn(10);
		when(readingCompletionService.processReadingCompletion(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, null))
			.thenReturn(120L);
		when(streakService.updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID)).thenReturn(false);

		// when
		progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

		// then
		verify(streakService).addStudyTime(TEST_USER_ID, 120L);
		verify(streakService).updateStreak(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID);
		verify(streakService).addCompletedContent(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, false);
	}

	@Test
	@DisplayName("마지막 청크여도 읽기 시간이 30초 미만이면 스트릭 관련 메서드를 호출하지 않는다")
	void updateProgress_shortReadTime_skipsStreakUpdates() {
		// given
		ProgressUpdateRequest request = new ProgressUpdateRequest();
		request.setChunkId(TEST_CHUNK_ID);

		when(bookService.existsById(TEST_BOOK_ID)).thenReturn(true);
		when(chunkService.findById(TEST_CHUNK_ID)).thenReturn(testChunk);
		when(chapterService.findById(TEST_CHAPTER_ID)).thenReturn(testChapter);
		when(bookProgressRepository.findByUserIdAndBookId(TEST_USER_ID, TEST_BOOK_ID))
			.thenReturn(Optional.of(testProgress));
		when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1)).thenReturn(5L);
		when(chapterRepository.countByBookId(TEST_BOOK_ID)).thenReturn(10);
		when(readingCompletionService.processReadingCompletion(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, null))
			.thenReturn(29L);

		// when
		progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

		// then
		verify(streakService, never()).addStudyTime(any(), anyLong());
		verify(streakService, never()).updateStreak(any(), any(), any());
		verify(streakService, never()).addCompletedContent(any(), any(), any(), anyBoolean());
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
		when(chunkRepository.countByChapterIdAndDifficultyLevel(TEST_CHAPTER_ID, DifficultyLevel.B1)).thenReturn(5L); // 총
																														// 5개
																														// 중
																														// 3번째
		when(chapterRepository.countByBookId(TEST_BOOK_ID)).thenReturn(10);
		when(readingCompletionService.processReadingCompletion(TEST_USER_ID, ContentType.BOOK, TEST_CHAPTER_ID, null))
			.thenReturn(null); // 마지막 청크가 아니므로 세션 처리 없음

		// when
		progressService.updateProgress(TEST_BOOK_ID, request, TEST_USER_ID);

		// then - 마지막 청크가 아니므로 세 메서드 모두 호출 안됨
		verify(streakService, never()).addStudyTime(any(), anyLong());
		verify(streakService, never()).addCompletedContent(any(), any(), any(), anyBoolean());
		verify(streakService, never()).updateStreak(any(), any(), any());
	}

}
