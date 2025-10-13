package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
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
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookProgressRepository bookProgressRepository;
    private final ChunkRepository chunkRepository;
    private final BookService bookService;

    public PageResponse<ChapterResponse> getChapters(String bookId, GetChaptersRequest request, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getLimit(),
            Sort.by("chapterNumber").ascending()
        );

        // Custom Repository 사용 - 필터링 + 페이지네이션 통합 처리
        Page<Chapter> chapterPage = chapterRepository.findChaptersWithFilters(bookId, request, userId, pageable);

        List<ChapterResponse> chapterResponses = chapterPage.getContent().stream()
            .map(chapter -> convertToChapterResponse(chapter, bookId, userId))
            .collect(Collectors.toList());

        return new PageResponse<>(chapterResponses, chapterPage);
    }

    public ChapterResponse getChapter(String bookId, String chapterId, String userId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        return convertToChapterResponse(chapter, bookId, userId);
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



    private ChapterResponse convertToChapterResponse(Chapter chapter, String bookId, String userId) {
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;

        // Book 조회 (currentDifficultyLevel fallback용)
        Book book = bookService.findById(bookId);
        DifficultyLevel currentDifficultyLevel = book.getDifficultyLevel(); // Fallback: Book의 난이도

        if (userId != null) {
            BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(null);

            if (bookProgress != null) {
                Integer currentChapterNumber = bookProgress.getCurrentReadChapterNumber() != null
                    ? bookProgress.getCurrentReadChapterNumber() : 0;

                // [DTO_MAPPING] chunk에서 chunkNumber 조회 (안전하게 처리)
                Integer currentChunkNumber = chunkRepository.findById(bookProgress.getChunkId())
                    .map(chunk -> chunk.getChunkNumber() != null ? chunk.getChunkNumber() : 0)
                    .orElse(0);

                if (chapter.getChapterNumber() < currentChapterNumber) {
                    // 현재 읽고 있는 챕터보다 이전 → 100% (이미 지나감)
                    currentReadChunkNumber = chapter.getChunkCount();
                    progressPercentage = 100.0;
                } else if (chapter.getChapterNumber().equals(currentChapterNumber)) {
                    // 현재 읽고 있는 챕터 → 백분율 계산
                    currentReadChunkNumber = currentChunkNumber;
                    if (chapter.getChunkCount() != null && chapter.getChunkCount() > 0) {
                        progressPercentage = (double) currentChunkNumber / chapter.getChunkCount() * 100.0;
                    }
                } else {
                    // 아직 안 읽은 챕터 → 0%
                    currentReadChunkNumber = 0;
                    progressPercentage = 0.0;
                }

                // Progress가 있으면 currentDifficultyLevel 사용
                if (bookProgress.getCurrentDifficultyLevel() != null) {
                    currentDifficultyLevel = bookProgress.getCurrentDifficultyLevel();
                }
            }
        }

        return ChapterResponse.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .chapterImageUrl(chapter.getChapterImageUrl())
            .description(chapter.getDescription())
            .chunkCount(chapter.getChunkCount())
            .currentReadChunkNumber(currentReadChunkNumber)
            .progressPercentage(progressPercentage)
            .currentDifficultyLevel(currentDifficultyLevel)
            .readingTime(chapter.getReadingTime())
            .build();
    }
}
