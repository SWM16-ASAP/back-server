package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.ChunkResponse;
import com.linglevel.api.books.dto.GetChunksRequest;
import com.linglevel.api.books.entity.Chapter;
import com.linglevel.api.books.entity.Chunk;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.repository.ChunkRepository;
import com.linglevel.api.common.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BookService bookService;
    private final ChapterService chapterService;

    public PageResponseDTO<ChunkResponse> getChunks(String bookId, String chapterId, GetChunksRequest request) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterService.findById(chapterId);
        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 50),
            Sort.by("chunkNumber").ascending()
        );

        Page<Chunk> chunkPage = chunkRepository.findByChapterId(chapterId, pageable);
        
        List<ChunkResponse> chunkResponses = chunkPage.getContent().stream()
            .map(this::convertToChunkResponse)
            .collect(Collectors.toList());

        return new PageResponseDTO<>(chunkResponses, chunkPage);
    }

    public ChunkResponse getChunk(String bookId, String chapterId, String chunkId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterService.findById(chapterId);
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
            .content(chunk.getContent())
            .isImage(chunk.getIsImage())
            .chunkImageUrl(chunk.getChunkImageUrl())
            .description(chunk.getDescription())
            .build();
    }
} 