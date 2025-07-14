package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.ProgressResponse;
import com.linglevel.api.books.dto.ProgressUpdateRequest;
import com.linglevel.api.books.entity.Book;
import com.linglevel.api.books.entity.BookProgress;
import com.linglevel.api.books.entity.Chapter;
import com.linglevel.api.books.entity.Chunk;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.repository.BookProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final BookService bookService;
    private final ChapterService chapterService;
    private final ChunkService chunkService;
    private final BookProgressRepository bookProgressRepository;

    @Transactional
    public ProgressResponse updateProgress(String bookId, ProgressUpdateRequest request) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterService.findById(request.getChapterId());
        Chunk chunk = chunkService.findById(request.getChunkId());
        String userId = "mock"; // TODO: 실제 유저 아이디 기록

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(new BookProgress());

        bookProgress.setUserId(userId);
        bookProgress.setBookId(bookId);
        bookProgress.setChapterId(request.getChapterId());
        bookProgress.setChunkId(request.getChunkId());
        bookProgress.setCurrentReadChapterNumber(chapter.getChapterNumber());
        bookProgress.setCurrentReadChunkNumber(chunk.getChunkNumber());

        bookProgressRepository.save(bookProgress);

        return convertToProgressResponse(bookProgress);
    }

    @Transactional
    public ProgressResponse getProgress(String bookId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        // TODO: 실제 유저 아이디 기록
        String userId = "mock";

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> initializeProgress(userId, bookId));

        return convertToProgressResponse(bookProgress);
    }

    private BookProgress initializeProgress(String userId, String bookId) {
        Chapter firstChapter = chapterService.findFirstByBookId(bookId);
        Chunk firstChunk = chunkService.findFirstByChapterId(firstChapter.getId());
        BookProgress newProgress = new BookProgress(null,
                userId,
                bookId,
                firstChapter.getId(),
                firstChunk.getId(),
                firstChapter.getChapterNumber(),
                firstChunk.getChunkNumber());

        return bookProgressRepository.save(newProgress);
    }

    private ProgressResponse convertToProgressResponse(BookProgress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .bookId(progress.getBookId())
                .chapterId(progress.getChapterId())
                .chunkId(progress.getChunkId())
                .currentReadChapterNumber(progress.getCurrentReadChapterNumber())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .build();
    }
} 