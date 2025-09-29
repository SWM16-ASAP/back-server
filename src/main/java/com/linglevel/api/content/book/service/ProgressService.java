package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ProgressResponse;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.exception.UsersErrorCode;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public ProgressResponse updateProgress(String bookId, ProgressUpdateRequest request, String username) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

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

        String userId = user.getId();

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(new BookProgress());

        bookProgress.setUserId(userId);
        bookProgress.setBookId(bookId);
        bookProgress.setChapterId(chapter.getId()); // 역추산된 chapter ID
        bookProgress.setChunkId(request.getChunkId());
        bookProgress.setCurrentReadChapterNumber(chapter.getChapterNumber());
        bookProgress.setCurrentReadChunkNumber(chunk.getChunkNumber());

        bookProgressRepository.save(bookProgress);

        return convertToProgressResponse(bookProgress);
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(String bookId, String username) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

        String userId = user.getId();

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

        return bookProgressRepository.save(newProgress);
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
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
} 