package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
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

        // Null 체크
        if (chapter.getChapterNumber() == null || chunk.getChunkNumber() == null) {
            throw new BooksException(BooksErrorCode.INVALID_CHUNK_NUMBER);
        }

        bookProgress.setUserId(userId);
        bookProgress.setBookId(bookId);
        bookProgress.setChapterId(chapter.getId()); // 역추산된 chapter ID
        bookProgress.setChunkId(request.getChunkId());
        bookProgress.setCurrentReadChapterNumber(chapter.getChapterNumber());
        bookProgress.setCurrentReadChunkNumber(chunk.getChunkNumber());

        // max 진도 업데이트 (current가 max보다 크면 max도 업데이트)
        if (shouldUpdateMaxProgress(bookProgress, chapter.getChapterNumber(), chunk.getChunkNumber())) {
            bookProgress.setMaxReadChapterNumber(chapter.getChapterNumber());
            bookProgress.setMaxReadChunkNumber(chunk.getChunkNumber());
        }

        // 완료 조건: 마지막 챕터의 마지막 청크 이상을 읽으면 완료
        if (bookService.existsById(bookId)) {
            var book = bookService.findById(bookId);
            boolean isLastChapter = chapter.getChapterNumber() >= book.getChapterCount();
            boolean isLastChunk = chunk.getChunkNumber() >= chapter.getChunkCount();
            boolean isCompleted = isLastChapter && isLastChunk;
            bookProgress.setIsCompleted(bookProgress.getIsCompleted() != null && bookProgress.getIsCompleted() || isCompleted);
        }

        bookProgressRepository.save(bookProgress);

        return convertToProgressResponse(bookProgress);
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
        newProgress.setCurrentReadChunkNumber(firstChunk.getChunkNumber());
        newProgress.setMaxReadChapterNumber(firstChapter.getChapterNumber());
        newProgress.setMaxReadChunkNumber(firstChunk.getChunkNumber());

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

    private boolean shouldUpdateMaxProgress(BookProgress progress, Integer chapterNum, Integer chunkNum) {
        Integer maxChapter = progress.getMaxReadChapterNumber();
        Integer maxChunk = progress.getMaxReadChunkNumber();

        return maxChapter == null || chapterNum > maxChapter
            || (chapterNum.equals(maxChapter) && chunkNum > maxChunk);
    }

    private ProgressResponse convertToProgressResponse(BookProgress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .bookId(progress.getBookId())
                .chapterId(progress.getChapterId())
                .chunkId(progress.getChunkId())
                .currentReadChapterNumber(progress.getCurrentReadChapterNumber())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .maxReadChapterNumber(progress.getMaxReadChapterNumber())
                .maxReadChunkNumber(progress.getMaxReadChunkNumber())
                .isCompleted(progress.getIsCompleted())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
} 