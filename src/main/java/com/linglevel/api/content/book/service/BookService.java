package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.*;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.service.ImageResizeService;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final BookProgressRepository bookProgressRepository;
    private final UserRepository userRepository;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final BookPathStrategy bookPathStrategy;
    private final ImageResizeService imageResizeService;

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

        if (StringUtils.hasText(coverImageUrl)) {
            try {
                log.info("Auto-processing cover image for imported book: {}", savedBook.getId());

                String originalCoverS3Key = bookPathStrategy.generateCoverImagePath(savedBook.getId());
                String smallImageUrl = imageResizeService.createSmallImage(originalCoverS3Key);

                savedBook.setCoverImageUrl(smallImageUrl);
                log.info("Successfully auto-processed cover image: {} → {}", savedBook.getId(), smallImageUrl);

            } catch (Exception e) {
                log.warn("Failed to auto-process cover image for book: {}, keeping original URL", savedBook.getId(), e);
            }
        }

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

    public PageResponse<BookResponse> getBooks(GetBooksRequest request, String username) {
        Sort sort = createSort(request.getSortBy());

        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getLimit(),
            sort
        );

        // 사용자 ID 조회
        final String userId = getUserId(username);

        // QueryDSL Custom Repository를 사용하여 필터링 + 페이지네이션 통합 처리
        Page<Book> bookPage = bookRepository.findBooksWithFilters(request, userId, pageable);

        List<BookResponse> bookResponses = bookPage.getContent().stream()
            .map(book -> convertToBookResponse(book, userId))
            .collect(Collectors.toList());

        return new PageResponse<>(bookResponses, bookPage);
    }

    public BookResponse getBook(String bookId, String username) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
        
        String userId = getUserId(username);
        return convertToBookResponse(book, userId);
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

    private List<BookResponse> filterByProgress(List<BookResponse> bookResponses, ProgressStatus progressFilter) {
        if (progressFilter == null) {
            return bookResponses; // No filter, return all
        }

        return bookResponses.stream()
            .filter(book -> {
                return switch (progressFilter) {
                    case NOT_STARTED -> book.getProgressPercentage() == 0.0;
                    case IN_PROGRESS -> book.getProgressPercentage() > 0.0 && !book.getIsCompleted();
                    case COMPLETED -> book.getIsCompleted();
                };
            })
            .collect(Collectors.toList());
    }

    private BookResponse convertToBookResponse(Book book, String userId) {
        // 진도 정보 조회
        int currentReadChapterNumber = 0;
        double progressPercentage = 0.0;
        boolean isCompleted = false;

        if (userId != null) {
            BookProgress progress = bookProgressRepository
                .findByUserIdAndBookId(userId, book.getId())
                .orElse(null);

            if (progress != null) {
                currentReadChapterNumber = progress.getCurrentReadChapterNumber() != null
                    ? progress.getCurrentReadChapterNumber() : 0;

                // 진행률 계산
                if (book.getChapterCount() != null && book.getChapterCount() > 0) {
                    progressPercentage = (double) currentReadChapterNumber / book.getChapterCount() * 100.0;
                }

                // DB에 저장된 완료 여부 사용
                isCompleted = progress.getIsCompleted() != null ? progress.getIsCompleted() : false;
            }
        }
        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .author(book.getAuthor())
            .coverImageUrl(book.getCoverImageUrl())
            .difficultyLevel(book.getDifficultyLevel())
            .chapterCount(book.getChapterCount())
            .currentReadChapterNumber(currentReadChapterNumber)
            .progressPercentage(progressPercentage)
            .isCompleted(isCompleted)
            .readingTime(book.getReadingTime())
            .averageRating(book.getAverageRating())
            .reviewCount(book.getReviewCount())
            .viewCount(book.getViewCount())
            .tags(book.getTags())
            .createdAt(book.getCreatedAt())
            .build();
    }

    private String getUserId(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElse(null);
    }
}