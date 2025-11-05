package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.streak.service.ReadingSessionService;
import com.linglevel.api.streak.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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
    private final ChapterRepository chapterRepository;


    @Transactional
    public ProgressResponse updateProgress(String bookId, ProgressUpdateRequest request, String userId) {
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

        ensureMigrated(bookProgress);

        // Null 체크
        if (chapter.getChapterNumber() == null || chunk.getChunkNumber() == null) {
            throw new BooksException(BooksErrorCode.INVALID_CHUNK_NUMBER);
        }

        bookProgress.setUserId(userId);
        bookProgress.setBookId(bookId);
        bookProgress.setChapterId(chapter.getId()); // 역추산된 chapter ID
        bookProgress.setChunkId(request.getChunkId());
        bookProgress.setCurrentReadChapterNumber(chapter.getChapterNumber());
        bookProgress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

        // [V3_CHAPTER_BASED] 챕터별 진행률 계산
        long totalChunksInChapter = chunkRepository.countByChapterIdAndDifficultyLevel(
            chapter.getId(), chunk.getDifficultyLevel()
        );
        double chapterProgressPercentage = totalChunksInChapter > 0
            ? (chunk.getChunkNumber() * 100.0 / totalChunksInChapter)
            : 0.0;

        // 챕터 진행률 배열 초기화 (null 체크)
        if (bookProgress.getChapterProgresses() == null) {
            bookProgress.setChapterProgresses(new ArrayList<>());
        }

        // 현재 챕터의 진행률 업데이트 (배열 구조)
        updateOrAddChapterProgress(bookProgress, chapter.getChapterNumber(), chapterProgressPercentage, false, null);

        // 책 전체 진행률 = 완료된 챕터 수 / 전체 챕터 수
        Integer totalChapters = chapterRepository.countByBookId(bookId);
        long completedCount = getCompletedChapterCount(bookProgress);
        double bookProgress_normalizedProgress = totalChapters > 0
            ? (completedCount * 100.0 / totalChapters)
            : 0.0;

        bookProgress.setNormalizedProgress(bookProgress_normalizedProgress);

        // max 진도 업데이트 (현재 읽고 있는 챕터 번호 기준)
        Integer currentChapterNum = chapter.getChapterNumber();
        Integer maxChapterNum = bookProgress.getMaxReadChapterNumber();

        if (maxChapterNum == null || currentChapterNum > maxChapterNum) {
            bookProgress.setMaxReadChapterNumber(currentChapterNum);
        }

        // maxNormalizedProgress는 완료된 챕터 기반으로 설정
        bookProgress.setMaxNormalizedProgress(bookProgress_normalizedProgress);

        // 스트릭 검사 및 완료 처리 로직
        boolean streakUpdated = false;
        if (isLastChunkInChapter(chunk)) {
            // 1. 챕터 완료 처리
            BookProgress.ChapterProgressInfo existingProgress = findChapterProgress(bookProgress, chapter.getChapterNumber());
            boolean isFirstCompletion = existingProgress == null || !Boolean.TRUE.equals(existingProgress.getIsCompleted());

            // 챕터 진행률을 100%로 설정하고 완료 처리
            updateOrAddChapterProgress(
                bookProgress,
                chapter.getChapterNumber(),
                100.0,
                true,
                isFirstCompletion ? java.time.Instant.now() : existingProgress.getCompletedAt()
            );

            log.info("Chapter {} completed for book {} (first completion: {})",
                chapter.getChapterNumber(), bookId, isFirstCompletion);

            streakService.addStudyTime(userId, readingSessionService.getReadingSessionSeconds(userId, ContentType.BOOK, chapter.getId()));
            streakUpdated = streakService.updateStreak(userId, ContentType.BOOK, chapter.getId());
            streakService.addCompletedContent(userId, ContentType.BOOK, chapter.getId(), streakUpdated);

            // 3. 책 전체 완료 확인 (모든 챕터 완료 시)
            boolean allChaptersCompleted = getCompletedChapterCount(bookProgress) >= totalChapters;
            if (allChaptersCompleted && bookProgress.getCompletedAt() == null) {
                bookProgress.setIsCompleted(true);
                bookProgress.setCompletedAt(java.time.Instant.now());
                log.info("Book {} fully completed (all {} chapters) by user {}", bookId, totalChapters, userId);
            }
        }

        readingSessionService.deleteReadingSession(userId);
        bookProgressRepository.save(bookProgress);

        return convertToProgressResponse(bookProgress, streakUpdated);
    }

    /**
     * 현재 청크가 챕터의 마지막 청크인지 확인
     */
    private boolean isLastChunkInChapter(Chunk chunk) {
        long totalChunks = chunkRepository.countByChapterIdAndDifficultyLevel(
            chunk.getChapterId(), chunk.getDifficultyLevel()
        );
        return chunk.getChunkNumber() >= totalChunks;
    }

    /**
     * 챕터 진행률 정보 찾기
     */
    private BookProgress.ChapterProgressInfo findChapterProgress(BookProgress bookProgress, Integer chapterNumber) {
        if (bookProgress.getChapterProgresses() == null) {
            return null;
        }
        return bookProgress.getChapterProgresses().stream()
            .filter(cp -> chapterNumber.equals(cp.getChapterNumber()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 챕터 진행률 업데이트 또는 추가
     */
    private void updateOrAddChapterProgress(
        BookProgress bookProgress,
        Integer chapterNumber,
        Double progressPercentage,
        Boolean isCompleted,
        java.time.Instant completedAt
    ) {
        BookProgress.ChapterProgressInfo existing = findChapterProgress(bookProgress, chapterNumber);

        if (existing != null) {
            // 기존 항목 업데이트
            existing.setProgressPercentage(progressPercentage);
            existing.setIsCompleted(isCompleted);
            if (completedAt != null) {
                existing.setCompletedAt(completedAt);
            }
        } else {
            // 새 항목 추가
            BookProgress.ChapterProgressInfo newProgress = BookProgress.ChapterProgressInfo.builder()
                .chapterNumber(chapterNumber)
                .progressPercentage(progressPercentage)
                .isCompleted(isCompleted)
                .completedAt(completedAt)
                .build();
            bookProgress.getChapterProgresses().add(newProgress);
        }
    }

    /**
     * 완료된 챕터 수 계산
     */
    private long getCompletedChapterCount(BookProgress bookProgress) {
        if (bookProgress.getChapterProgresses() == null) {
            return 0;
        }
        return bookProgress.getChapterProgresses().stream()
            .filter(cp -> Boolean.TRUE.equals(cp.getIsCompleted()))
            .count();
    }

    /**
     * V3 마이그레이션 보장
     * updateProgress 시점에 한 번만 실행
     */
    private void ensureMigrated(BookProgress progress) {
        boolean needsMigration = false;

        // V3 필드 초기화
        if (progress.getChapterProgresses() == null) {
            progress.setChapterProgresses(new ArrayList<>());
            needsMigration = true;
        }

        if (needsMigration) {
            log.info("V3 migration completed for BookProgress id={}, userId={}",
                progress.getId(), progress.getUserId());
        }
    }


    @Transactional(readOnly = true)
    public ProgressResponse getProgress(String bookId, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> initializeProgress(userId, bookId));

        return convertToProgressResponse(bookProgress, false);
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

    private ProgressResponse convertToProgressResponse(BookProgress progress, boolean streakUpdated) {
        // [DTO_MAPPING] chunk에서 chunkNumber 조회
        Chunk chunk = chunkService.findById(progress.getChunkId());

        // [SAFETY] 마이그레이션이 안 되어 있는 경우 경고 로그
        if (progress.getChapterProgresses() == null) {
            log.warn("BookProgress {} not migrated yet - this should only happen on read-only access",
                progress.getId());
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
                .streakUpdated(streakUpdated)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
} 