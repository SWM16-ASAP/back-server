package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.*;
import com.linglevel.api.books.entity.Book;
import com.linglevel.api.books.entity.Chapter;
import com.linglevel.api.books.entity.DifficultyLevel;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.repository.BookRepository;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final BookPathStrategy bookPathStrategy;
    
    private final BookReadingTimeService bookReadingTimeService;
    private final BookImportService bookImportService;

    @Transactional
    public BookImportResponse importBook(BookImportRequest request) {
        log.info("Starting book import for file: {}", request.getId());
        BookImportData importData = s3AiService.downloadJsonFile(request.getId(), BookImportData.class, bookPathStrategy);

        Book book = createBook(importData, request.getId());
        Book savedBook = bookRepository.save(book);
        
        s3TransferService.transferImagesFromAiToStatic(request.getId(), savedBook.getId(), bookPathStrategy);

        String coverImageUrl = s3UrlService.getCoverImageUrl(savedBook.getId(), bookPathStrategy);
        savedBook.setCoverImageUrl(coverImageUrl);
        bookRepository.save(savedBook);
        
        List<Chapter> savedChapters = bookImportService.createChaptersFromMetadata(importData, savedBook.getId());
        
        bookImportService.createChunksFromLeveledResults(importData, savedChapters, savedBook.getId());
        
        bookReadingTimeService.updateReadingTimes(savedBook.getId(), importData);
        
        log.info("Successfully imported book with id: {}", savedBook.getId());
        return new BookImportResponse(savedBook.getId());
    }

    private Book createBook(BookImportData importData, String requestId) {
        Book book = new Book();
        book.setTitle(importData.getTitle());
        book.setAuthor(importData.getAuthor());
        DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(importData
                .getOriginalTextLevel().toUpperCase());
        book.setDifficultyLevel(difficultyLevel);

        String coverImageUrl = s3UrlService.getCoverImageUrl(requestId, bookPathStrategy);
        book.setCoverImageUrl(coverImageUrl);
        
        book.setViewCount(0);
        book.setAverageRating(0.0);
        book.setReviewCount(0);
        book.setReadingTime(0);
        book.setCreatedAt(LocalDateTime.now());
        
        int chapterCount = importData.getLeveledResults().isEmpty() ? 0 :
                          importData.getLeveledResults().get(0).getChapters().size();

        book.setChapterCount(chapterCount);
        
        return book;
    }

    public PageResponse<BookResponse> getBooks(GetBooksRequest request) {
        Sort sort = createSort(request.getSortBy());

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 50),
            sort
        );

        Page<Book> bookPage = bookRepository.findAll(pageable);

        List<BookResponse> bookResponses = bookPage.getContent().stream()
            .map(this::convertToBookResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(bookResponses, bookPage);
    }

    public BookResponse getBook(String bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
        
        return convertToBookResponse(book);
    }

    public boolean existsById(String bookId) { 
        return bookRepository.existsById(bookId); 
    }

    public Book findById(String bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
    }

    private Sort createSort(String sortBy) {
        if (sortBy == null) {
            sortBy = "created_at";
        }

        return switch (sortBy.toLowerCase()) {
            case "view_count" -> Sort.by("viewCount").descending();
            case "average_rating" -> Sort.by("averageRating").descending();
            case "created_at" -> Sort.by("createdAt").descending();
            default -> throw new BooksException(BooksErrorCode.INVALID_SORT_BY);
        };
    }

    private BookResponse convertToBookResponse(Book book) {
        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .author(book.getAuthor())
            .coverImageUrl(book.getCoverImageUrl())
            .difficultyLevel(book.getDifficultyLevel())
            .chapterCount(book.getChapterCount())
            .currentReadChapterNumber(0) // TODO: 실제 진도 계산
            .progressPercentage(0.0) // TODO: 실제 진도 계산
            .readingTime(book.getReadingTime())
            .averageRating(book.getAverageRating())
            .reviewCount(book.getReviewCount())
            .viewCount(book.getViewCount())
            .tags(book.getTags())
            .createdAt(book.getCreatedAt())
            .build();
    }
}