package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.streak.service.ReadingSessionService;
import com.linglevel.api.streak.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final BookService bookService;
    private final ChapterService chapterService;
    private final ChunkService chunkService;
    private final BookProgressRepository bookProgressRepository;
    private final ChunkRepository chunkRepository;
    private final ProgressCalculationService progressCalculationService;
    private final ReadingSessionService readingSessionService;
    private final StreakService streakService;


    @Transactional
    public ProgressResponse updateProgress(String bookId, ProgressUpdateRequest request, String userId) {
        readingSessionService.startReadingSession(userId, ContentType.BOOK, bookId);

        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        // chunkId로부터 chunk 정보 조회
        Chunk chunk = chunkService.findById(request.getChunkId());

        // chunk로부터 chapter 역추산
        if (chunk.getChapterId() == null) {
            throw new BooksException(BooksErrorCode.CHUNK_NOT_FOUND);
        }
        Chapter chapter = chapterService.findById(chunk.getChapterId());

        if (!chapter.getBookId().equals(bookId)) {
            throw new BooksException(BooksErrorCode.CHUNK_NOT_FOUND_IN_BOOK);
        }

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(new BookProgress());

        // Null 체크
        if (chapter.getChapterNumber() == null || chunk.getChunkNumber() == null) {
            throw new BooksException(BooksErrorCode.INVALID_CHUNK_NUMBER);
        }

        bookProgress.setUserId(userId);
        bookProgress.setBookId(bookId);
        bookProgress.setChapterId(chapter.getId()); // 역추산된 chapter ID
        bookProgress.setChunkId(request.getChunkId());
        bookProgress.setCurrentReadChapterNumber(chapter.getChapterNumber());

        // [V2_CORE] V2 필드: 정규화된 진행률 계산
        long totalChunks = chunkRepository.countByChapterIdAndDifficultyLevel(
            chapter.getId(), chunk.getDifficultyLevel()
        );
        double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
            chunk.getChunkNumber(), totalChunks
        );

        bookProgress.setNormalizedProgress(normalizedProgress);
        bookProgress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

        // max 진도 업데이트 로직 변경
        Integer currentChapterNum = chapter.getChapterNumber();
        Integer maxChapterNum = bookProgress.getMaxReadChapterNumber();

        if (maxChapterNum == null || currentChapterNum > maxChapterNum) {
            // 새로운 높은 챕터로 이동한 경우, max 값을 현재 값으로 덮어쓴다.
            bookProgress.setMaxReadChapterNumber(currentChapterNum);
            bookProgress.setMaxNormalizedProgress(normalizedProgress);
        } else if (currentChapterNum.equals(maxChapterNum)) {
            // 가장 높은 챕터 내에서 진행률이 증가한 경우, max 값을 갱신한다.
            if (progressCalculationService.shouldUpdateMaxProgress(
                bookProgress.getMaxNormalizedProgress(), normalizedProgress)) {
                bookProgress.setMaxNormalizedProgress(normalizedProgress);
            }
        }
        // 현재 챕터가 max 챕터보다 낮은 경우는 max 값을 변경하지 않는다.

        // 완료 조건 변경: 마지막 챕터의 진행률이 100%일 때만 완료 처리
        com.linglevel.api.content.book.entity.Book book = bookService.findById(bookId);
        boolean isLastChapter = chapter.getChapterNumber().equals(book.getChapterCount());
        boolean isChapterCompleted = progressCalculationService.isCompleted(normalizedProgress);

        if (isLastChapter && isChapterCompleted) {
            bookProgress.setIsCompleted(true);
        }

        // 스트릭 검사 로직
        if (isLastChunk(chunk) && readingSessionService.isReadingSessionValid(userId, ContentType.BOOK, bookId) && !streakService.hasCompletedStreakToday(userId)) {
            log.info("Streak condition met for user: {}", userId);
            // TODO: Implement streak update logic
            readingSessionService.deleteReadingSession(userId);
        }

        bookProgressRepository.save(bookProgress);

        return convertToProgressResponse(bookProgress);
    }

    private boolean isLastChunk(Chunk chunk) {
        long totalChunks = chunkRepository.countByChapterIdAndDifficultyLevel(
            chunk.getChapterId(), chunk.getDifficultyLevel()
        );
        return chunk.getChunkNumber() >= totalChunks;
    }


    @Transactional(readOnly = true)
    public ProgressResponse getProgress(String bookId, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> initializeProgress(userId, bookId));

        return convertToProgressResponse(bookProgress);
    }

    private BookProgress initializeProgress(String userId, String bookId) {
        Chapter firstChapter = chapterService.findFirstByBookId(bookId);
        Chunk firstChunk = chunkService.findFirstByChapterId(firstChapter.getId());

        BookProgress newProgress = new BookProgress();
        newProgress.setUserId(userId);
        newProgress.setBookId(bookId);
        newProgress.setChapterId(firstChapter.getId());
        newProgress.setChunkId(firstChunk.getId());
        newProgress.setCurrentReadChapterNumber(firstChapter.getChapterNumber());
        newProgress.setMaxReadChapterNumber(firstChapter.getChapterNumber());

        // [V2_CORE] V2 필드: 초기 진행률 계산
        long totalChunks = chunkRepository.countByChapterIdAndDifficultyLevel(
            firstChapter.getId(), firstChunk.getDifficultyLevel()
        );
        double initialProgress = progressCalculationService.calculateNormalizedProgress(
            firstChunk.getChunkNumber(), totalChunks
        );

        newProgress.setNormalizedProgress(initialProgress);
        newProgress.setMaxNormalizedProgress(initialProgress);
        newProgress.setCurrentDifficultyLevel(firstChunk.getDifficultyLevel());

        return bookProgressRepository.save(newProgress);
    }

    @Transactional
    public void deleteProgress(String bookId, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.PROGRESS_NOT_FOUND));

        bookProgressRepository.delete(bookProgress);
    }

    // [V1_COMPAT] Chapter 기반 max 진도만 관리
    private boolean shouldUpdateMaxProgress(BookProgress progress, Integer chapterNum) {
        Integer maxChapter = progress.getMaxReadChapterNumber();
        return maxChapter == null || chapterNum > maxChapter;
    }

    private ProgressResponse convertToProgressResponse(BookProgress progress) {
        // [DTO_MAPPING] chunk에서 chunkNumber 조회
        Chunk chunk = chunkService.findById(progress.getChunkId());

        // [FALLBACK] V2 필드가 없으면 동적 계산 (기존 데이터 대응)
        if (progress.getNormalizedProgress() == null || progress.getCurrentDifficultyLevel() == null) {
            log.info("V2 fields missing for BookProgress {}, calculating lazily", progress.getId());

            // 난이도별 전체 청크 수 조회
            long totalChunks = chunkRepository.countByChapterIdAndDifficultyLevel(
                chunk.getChapterId(), chunk.getDifficultyLevel()
            );
            double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
                chunk.getChunkNumber(), totalChunks
            );

            // Lazy migration: V2 필드 저장
            progress.setNormalizedProgress(normalizedProgress);
            progress.setMaxNormalizedProgress(normalizedProgress);
            progress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

            bookProgressRepository.save(progress);
            log.info("Lazy migration completed for BookProgress {}", progress.getId());
        }

        return ProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .bookId(progress.getBookId())
                .chapterId(progress.getChapterId())
                .chunkId(progress.getChunkId())
                .currentReadChapterNumber(progress.getCurrentReadChapterNumber())
                .currentReadChunkNumber(chunk.getChunkNumber())
                .maxReadChapterNumber(progress.getMaxReadChapterNumber())
                .isCompleted(progress.getIsCompleted())
                .currentDifficultyLevel(progress.getCurrentDifficultyLevel())
                .normalizedProgress(progress.getNormalizedProgress())
                .maxNormalizedProgress(progress.getMaxNormalizedProgress())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
} 