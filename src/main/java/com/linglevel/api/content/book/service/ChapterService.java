package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ChapterNavigationResponse;
import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.ChunkCountByLevelDto;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookProgressRepository bookProgressRepository;
    private final ChunkRepository chunkRepository;
    private final BookService bookService;
    private final BookRepository bookRepository;

    public PageResponse<ChapterResponse> getChapters(String bookId, GetChaptersRequest request, String userId) {
        Book book = bookService.findById(bookId);

        bookRepository.incrementViewCount(bookId);

        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getLimit(),
            Sort.by("chapterNumber").ascending()
        );

        Page<Chapter> chapterPage = chapterRepository.findChaptersWithFilters(bookId, request, userId, pageable);
        List<Chapter> chapters = chapterPage.getContent();

        if (chapters.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), chapterPage);
        }

        List<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());

        BookProgress bookProgress = Optional.ofNullable(userId)
            .flatMap(id -> bookProgressRepository.findByUserIdAndBookId(id, bookId))
            .orElse(null);

        Chunk progressChunk = (bookProgress != null && bookProgress.getChunkId() != null)
            ? chunkRepository.findById(bookProgress.getChunkId()).orElse(null)
            : null;

        Map<String, Map<DifficultyLevel, Long>> chunkCountsMap = chunkRepository.findChunkCountsByChapterIds(chapterIds)
            .stream()
            .collect(Collectors.groupingBy(
                ChunkCountByLevelDto::getChapterId,
                Collectors.toMap(ChunkCountByLevelDto::getDifficultyLevel, ChunkCountByLevelDto::getCount)
            ));

        List<ChapterResponse> chapterResponses = chapters.stream()
            .map(chapter -> convertToChapterResponse(chapter, book, bookProgress, progressChunk, chunkCountsMap))
            .collect(Collectors.toList());

        return new PageResponse<>(chapterResponses, chapterPage);
    }

    public ChapterResponse getChapter(String bookId, String chapterId, String userId) {
        Book book = bookService.findById(bookId);

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        BookProgress bookProgress = Optional.ofNullable(userId)
            .flatMap(id -> bookProgressRepository.findByUserIdAndBookId(id, bookId))
            .orElse(null);

        Chunk progressChunk = (bookProgress != null && bookProgress.getChunkId() != null)
            ? chunkRepository.findById(bookProgress.getChunkId()).orElse(null)
            : null;

        Map<String, Map<DifficultyLevel, Long>> chunkCountsMap = chunkRepository.findChunkCountsByChapterIds(Collections.singletonList(chapterId))
            .stream()
            .collect(Collectors.groupingBy(
                ChunkCountByLevelDto::getChapterId,
                Collectors.toMap(ChunkCountByLevelDto::getDifficultyLevel, ChunkCountByLevelDto::getCount)
            ));

        return convertToChapterResponse(chapter, book, bookProgress, progressChunk, chunkCountsMap);
    }

    public boolean existsById(String chapterId) {
        return chapterRepository.existsById(chapterId);
    }

    public Chapter findById(String chapterId) {
        return chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
    }

    public Chapter findFirstByBookId(String bookId) {
        return chapterRepository.findFirstByBookIdOrderByChapterNumberAsc(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
    }

    public ChapterNavigationResponse getChapterNavigation(String bookId, String chapterId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter currentChapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        if (!bookId.equals(currentChapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        Optional<Chapter> previousChapter = chapterRepository.findByBookIdAndChapterNumber(
            bookId, currentChapter.getChapterNumber() - 1);

        Optional<Chapter> nextChapter = chapterRepository.findByBookIdAndChapterNumber(
            bookId, currentChapter.getChapterNumber() + 1);

        return ChapterNavigationResponse.builder()
            .currentChapterId(chapterId)
            .currentChapterNumber(currentChapter.getChapterNumber())
            .hasPreviousChapter(previousChapter.isPresent())
            .previousChapterId(previousChapter.map(Chapter::getId).orElse(null))
            .hasNextChapter(nextChapter.isPresent())
            .nextChapterId(nextChapter.map(Chapter::getId).orElse(null))
            .build();
    }

    private ChapterResponse convertToChapterResponse(Chapter chapter, Book book, BookProgress bookProgress, Chunk progressChunk, Map<String, Map<DifficultyLevel, Long>> chunkCountsMap) {
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;
        DifficultyLevel currentDifficultyLevel = book.getDifficultyLevel(); // Fallback: Book's difficulty

        if (bookProgress != null) {
            if (bookProgress.getCurrentDifficultyLevel() != null) {
                currentDifficultyLevel = bookProgress.getCurrentDifficultyLevel();
            }

            Integer progressChapterNumber = bookProgress.getCurrentReadChapterNumber() != null
                ? bookProgress.getCurrentReadChapterNumber() : 0;

            Integer progressChunkNumber = (progressChunk != null && progressChunk.getChunkNumber() != null)
                ? progressChunk.getChunkNumber() : 0;

            long totalChunksForLevel = chunkCountsMap.getOrDefault(chapter.getId(), Collections.emptyMap()).getOrDefault(currentDifficultyLevel, 0L);

            if (chapter.getChapterNumber() < progressChapterNumber) {
                currentReadChunkNumber = (int) totalChunksForLevel;
                progressPercentage = 100.0;
            } else if (chapter.getChapterNumber().equals(progressChapterNumber)) {
                currentReadChunkNumber = progressChunkNumber;
                if (totalChunksForLevel > 0) {
                    progressPercentage = (double) progressChunkNumber / totalChunksForLevel * 100.0;
                }
            } else {
                currentReadChunkNumber = 0;
                progressPercentage = 0.0;
            }
        }

        long totalChunkCount = chunkCountsMap.getOrDefault(chapter.getId(), Collections.emptyMap()).getOrDefault(currentDifficultyLevel, 0L);

        return ChapterResponse.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .chapterImageUrl(chapter.getChapterImageUrl())
            .description(chapter.getDescription())
            .chunkCount((int) totalChunkCount)
            .currentReadChunkNumber(currentReadChunkNumber)
            .progressPercentage(progressPercentage)
            .currentDifficultyLevel(currentDifficultyLevel)
            .readingTime(chapter.getReadingTime())
            .build();
    }
}
