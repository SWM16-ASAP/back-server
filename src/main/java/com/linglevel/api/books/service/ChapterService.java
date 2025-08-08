package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.ChapterResponse;
import com.linglevel.api.books.dto.GetChaptersRequest;
import com.linglevel.api.books.entity.Chapter;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.repository.ChapterRepository;
import com.linglevel.api.common.dto.PageResponse;
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
    private final BookService bookService;

    public PageResponse<ChapterResponse> getChapters(String bookId, GetChaptersRequest request) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 50),
            Sort.by("chapterNumber").ascending()
        );

        Page<Chapter> chapterPage = chapterRepository.findByBookId(bookId, pageable);
        
        List<ChapterResponse> chapterResponses = chapterPage.getContent().stream()
            .map(this::convertToChapterResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(chapterResponses, chapterPage);
    }

    public ChapterResponse getChapter(String bookId, String chapterId) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        return convertToChapterResponse(chapter);
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

    private ChapterResponse convertToChapterResponse(Chapter chapter) {
        return ChapterResponse.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .chapterImageUrl(chapter.getChapterImageUrl())
            .description(chapter.getDescription())
            .chunkCount(chapter.getChunkCount())
            .currentReadChunkNumber(0) // TODO: 실제 진도 계산
            .progressPercentage(0.0) // TODO: 실제 진도 계산
            .readingTime(chapter.getReadingTime())
            .build();
    }
} 