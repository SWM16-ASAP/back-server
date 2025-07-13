package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.BookResponse;
import com.linglevel.api.books.dto.GetBooksRequest;
import com.linglevel.api.books.entity.Book;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.repository.BookRepository;
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
public class BookService {

    private final BookRepository bookRepository;

    public PageResponseDTO<BookResponse> getBooks(GetBooksRequest request) {
        Sort sort = createSort(request.getSortBy());

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 50), 
            sort
        );

        // 전체 책 조회 (TODO: 필터링 로직 추가)
        Page<Book> bookPage = bookRepository.findAll(pageable);

        List<BookResponse> bookResponses = bookPage.getContent().stream()
            .map(this::convertToBookResponse)
            .collect(Collectors.toList());

        return new PageResponseDTO<>(bookResponses, bookPage);
    }

    public BookResponse getBook(String bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
        
        return convertToBookResponse(book);
    }

    public boolean existsById(String bookId) { return bookRepository.existsById(bookId); }

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