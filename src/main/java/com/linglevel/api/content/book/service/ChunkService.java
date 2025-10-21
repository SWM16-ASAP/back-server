package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ChunkResponse;
import com.linglevel.api.content.book.dto.GetChunksRequest;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.recommendation.event.ContentAccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkService {

    private final ChunkRepository chunkRepository;
    private final ChapterRepository chapterRepository;
    private final BookService bookService;
    private final ApplicationEventPublisher eventPublisher;

    public PageResponse<ChunkResponse> getChunks(String bookId, String chapterId, GetChunksRequest request, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        // 사용자 콘텐츠 접근 로깅 (비동기)
        if (userId != null) {
            eventPublisher.publishEvent(new ContentAccessEvent(
                    this, userId, bookId, ContentType.BOOK, null  // Book은 category 없음
            ));
        }

        DifficultyLevel difficulty;
        try {
            difficulty = request.getDifficultyLevel();
        } catch (IllegalArgumentException e) {
            throw new BooksException(BooksErrorCode.INVALID_DIFFICULTY_LEVEL);
        }

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 200),
            Sort.by("chunkNumber").ascending()
        );

        Page<Chunk> chunkPage = chunkRepository.findByChapterIdAndDifficultyLevel(chapterId, difficulty, pageable);
        log.info("Found chunks count: {}", chunkPage.getTotalElements());
        
        List<ChunkResponse> chunkResponses = chunkPage.getContent().stream()
            .map(this::convertToChunkResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(chunkResponses, chunkPage);
    }

    public ChunkResponse getChunk(String bookId, String chapterId, String chunkId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        Chunk chunk = chunkRepository.findById(chunkId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHUNK_NOT_FOUND));

        if (!chapterId.equals(chunk.getChapterId())) {
            throw new BooksException(BooksErrorCode.CHUNK_NOT_FOUND);
        }

        return convertToChunkResponse(chunk);
    }

    public boolean existsById(String chunkId) { return chunkRepository.existsById(chunkId); }

    public Chunk findById(String chunkId) {
        return chunkRepository.findById(chunkId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.CHUNK_NOT_FOUND));
    }

    public Chunk findFirstByChapterId(String chapterId) {
        return chunkRepository.findFirstByChapterIdOrderByChunkNumberAsc(chapterId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.CHUNK_NOT_FOUND));
    }

    private ChunkResponse convertToChunkResponse(Chunk chunk) {
        return ChunkResponse.builder()
            .id(chunk.getId())
            .chunkNumber(chunk.getChunkNumber())
            .difficultyLevel(chunk.getDifficultyLevel())
            .type(chunk.getType())
            .content(chunk.getContent())
            .description(chunk.getDescription())
            .build();
    }
} 